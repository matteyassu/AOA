import java.io.*;
import java.util.*;
import java.lang.reflect.*;
/*
convert object to protobuffer format and write to txt file. Then stick txt file (cbor?) into bytebuffer somehow, then transfer over to hikey\
On hikey end, re-inflate txt to object and manipulate
 
*/
public class ProtoConverter{

   public static void main(String [] args)throws Exception{
      Astronaut a = new Astronaut();
      Rocket r = new Rocket(a,"FalconHeavy",5,10000,3);
      
      Map<String,String> h = new HashMap<>();
      h.put("Integer","int32");
      h.put("String","String");
      File f = convert(h,"rocket.txt",r,2,"NASA",false,"",false,"");
      Scanner scan = new Scanner(f);
      String terminalOutput = "";
      f.delete();
   }
   
   public static File convert(Map<String,String> namingConventions,String filename,Object o,int version,String pkgName,boolean java_package,String java_pkg,boolean outer_class_name,String outerName) throws Exception{
      File f = new File(filename);
      if(f.createNewFile())
         System.out.println("File created");
      else{
         f = new File(filename + Math.random()*10);
      }   
      FileWriter writer = new FileWriter(f);
      String s = "syntax = \"proto" + version + "\";\n\npackage " + pkgName + ";\n";
      if(java_package)
         s += "\n\noption java_package = \"" + java_pkg + "\";\n";
      if(outer_class_name)
         s+= "\noption java_outer_classname = \"" + outerName + "\";\n";
      
      s += "\nmessage " + (o.getClass() + "").substring(6) + " {\n";
      String output = generateAttributes(namingConventions,o,"");
      s += output + "\n}";
      System.out.println(s);
      writer.write(s);
      writer.close();
      return f;
   }
   private static String generateAttributes(Map<String,String> namingConventions, Object o,String s)throws Exception{
         int i = 0;
         List<Method> methods = Arrays.asList(o.getClass().getMethods());
         for(Method m : methods){
            if(m.getName().substring(0,3).equals("get") && m.getParameterTypes().length == 0){
               if(m.getName().equals("getClass"))
                  continue;
               String varName = " " + m.getName().substring(3,4).toLowerCase() + m.getName().substring(4) + " = " + i++ + ";\n";
               Object r = m.invoke(o);
               String protoKw = getProtoKeyword(namingConventions,r);
               boolean builtIn = (protoKw == null) ? false:true;
               if(builtIn)
                  s += protoKw + varName;
               else{
               //check whether obj from last method is builtIn;if yes swap,else process
               //below way better approach to get return type; no string manipulation
                  Method last = methods.get(methods.size()-1);
                  String lastRt = last.getReturnType().toString();
                  if(namingConventions.get(lastRt) != null){
                     methods.remove(m);
                     methods.add(m);
                  }
                  else{
                     s += "message " + (r.getClass() + "") + "{"; 
                     s += generateAttributes(namingConventions,r,"") + "}\n"; 
                  }
               }
            }
         }
         return s;
   }
   
   private static String getProtoKeyword(Map<String,String> namingConventions,Object r){
      String classo = r.getClass() + "";
      String dt = classo.substring(classo.lastIndexOf(".") + 1);
      String nm = namingConventions.get(dt);
      return nm;
      //return namingConventions.get(r.getClass() + "").substring((r.getClass() + "").lastIndexOf(".") + 1);
   }
}