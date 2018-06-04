package wingsby.common;

import wingsby.parsegrib.ElementName;

public class MergeTools {

   public static String  getGFSFromFC(String fcele){
       if(fcele.equals("TT"))return "TMPS";
       if(fcele.equals("WD"))return "WDS";
       if(fcele.equals("WS"))return "WSS";
       if(fcele.equals("RH"))return "RHS";
       if(fcele.equals("CN"))return "TCDC";
       if(fcele.equals("CNL"))return "TCDCL";
       if(fcele.equals("RN"))return "TR";
       return null;

   }
}
