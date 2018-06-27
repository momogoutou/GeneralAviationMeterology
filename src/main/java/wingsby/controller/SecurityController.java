package wingsby.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.codec.binary.Base64;
import org.apache.ibatis.session.SqlSession;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import redis.clients.jedis.JedisCluster;
import wingsby.common.tools.RSAUtil;
import wingsby.dao.XHMSUsers;
import wingsby.service.AviationMeterologyService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Controller
@RequestMapping
public class SecurityController {

//    @Autowired
//    private AviationMeterologyService iService;

    //注册流程
    //注册后邮箱验证（前端带图片） ？？
    //邮箱点击后后台验证 ？？
    //临时map
    @Autowired
    private JedisCluster jedisCluster;

    @Autowired
    private SqlSession sqlSession;

    private static BlockingQueue<JSONObject> keyPairs = new LinkedBlockingQueue<>();
    private static final int keyPairsLimit = 100;

    static {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (keyPairs.size() > keyPairsLimit) {
                        try {
                            Thread.sleep(600 * 1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        JSONObject keypair = RSAUtil.generateKeyPairs();
                        keyPairs.add(keypair);
                    }
                    try {
                        Thread.sleep(60 * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

    }


    @RequestMapping(value = "/register", method = RequestMethod.POST)
    @ResponseBody
    public JSONObject register(HttpServletRequest request, @RequestBody JSONObject json) {
        //临时验证
        String name = json.get("name").toString();
        String passwd = json.get("passwd").toString();
//        String email = json.get("email").toString();
        String sign = json.get("sign").toString();
        int flag = -1;
//        if (sign.isValid()) {
        //完成注册
        //生成密码对，并返回公钥
        long stime = System.currentTimeMillis();
        JSONObject keypair = keyPairs.poll();
        if (keypair == null)
            keypair = RSAUtil.generateKeyPairs();
//            json.put("public", keypair.get("public"));
//            json.put("private", keypair.get("private"));
        System.out.println(System.currentTimeMillis() - stime);
        XHMSUsers user = new XHMSUsers();
        user.setName(name);
        user.setPasswd(passwd);
        user.setPriKey(keypair.get("private").toString());
        user.setPubKey(keypair.get("public").toString());
        // 插入数据库即可
        flag = sqlSession.insert("wingsby.dao.XHMSUsers.insertUsers", user);
//        }
        JSONObject resObject = new JSONObject();
        if (flag > 0) {
            resObject.put("code", 0);
            resObject.put("pubKey", user.getPubKey());
        } else {
            resObject.put("code", 300);

        }
        return resObject;
    }


    @RequestMapping(value = "/login", method = RequestMethod.POST)
    @ResponseBody
    public JSONObject login(HttpServletRequest request, @RequestBody JSONObject json) {
        JSONObject resObject = new JSONObject();
        String name = json.get("name").toString();
        String passwd = json.get("passwd").toString();
        try {
            //明文 base64
            name = new String(Base64.decodeBase64(name), "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        XHMSUsers storeUser = sqlSession.selectOne("wingsby.dao.XHMSUsers.selectPriKeys", name);
        if (storeUser == null) {
            resObject.put("code", 305);
            return resObject;
        }
        PrivateKey key = RSAUtil.encodePriKey(storeUser.getPriKey());
        //秘钥解密
        String decryptPasswd = RSAUtil.decryptData(passwd, key);
        if (decryptPasswd.equals(storeUser.getPasswd())) {
            //生成token 并将其添加到redis
            resObject.put("code", 0);
            String token = UUID.randomUUID().toString().replace("-", "");
            String datestr = DateTime.now().toString("yyyyMMddHHmmss");
            token = token + "_" + datestr;
            jedisCluster.hset("XHMS_USERS", name, token);
            resObject.put("token", token);
            return resObject;
        } else {
            resObject.put("code", 304);
        }
        return null;

    }


}
