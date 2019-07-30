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
      h.put("int","int32");
      
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
         String methodName = m.getName(); 
         if(methodName.substring(0,3).equals("get") && m.getParameterTypes().length == 0){
            if(m.getName().equals("getClass"))
               continue;
               //gather components of method signature
               //ex:engineType
            String returnVar = methodName.substring(3,4).toLowerCase() + methodName.substring(4);
               //ex:String
            String returnType = m.getReturnType().toString();
            boolean builtIn = false;
            if(namingConventions.get(returnType) != null)
               builtIn = true;
            else if(returnType.contains(".")){
               //if comes in class java.lang.String format
               String type = returnType.substring(returnType.lastIndexOf(".")+1);
               if(namingConventions.get(type) != null)
                  builtIn = true;
            }
               //primitive
            if(builtIn)
               s += processPrimitive(namingConventions,returnType,returnVar,i);
               //complex
            else{
               int endIndex = methods.indexOf(m);
               s += processComplex(namingConventions,methods,o,"",endIndex); 
            }
         }
      }
      return s;
   }
   
   private static String processPrimitive(Map<String,String> namingConventions,String returnType,String returnVar,int i){
      return namingConventions.get(returnType) + " " + returnVar + " = " + i + ";\n";
   }
   
   // private static boolean builtIn(Map<String,String> namingConventions,String returnType){
//       
//    }
   private static String processComplex(Map<String,String> namingConventions,List<Method> methods,Object o,String s,int endIndex)throws Exception{
      //execute swapping algo and call processPrimitive() OR swapping not possible and execute processComplex()
      for(int i = methods.size()-1; i >= endIndex; i--){
         //if get(),0 param,and built in swap
         if(methods.get(i).getName().substring(0,3).equals("get") && methods.get(i).getParameterTypes().length == 0 && namingConventions.get(methods.get(i).getReturnType().toString()) != null){
            String returnType = methods.get(i).getReturnType().toString();
            boolean builtIn = false;
            if(namingConventions.get(returnType) != null)
               builtIn = true;
            else if(returnType.contains(".")){
               //if comes in class java.lang.String format
               String type = returnType.substring(returnType.lastIndexOf(".")+1);
               if(namingConventions.get(type) != null)
                  builtIn = true;
            }
            if(builtIn){
               swap(methods,endIndex,i);
               return processPrimitive(namingConventions,methods.get(endIndex).getReturnType().toString(),methods.get(endIndex).getName().substring(3,4).toLowerCase() + methods.get(endIndex).getName().substring(4),1);
            }
         }
      } 
      Object complexObj = methods.get(endIndex).invoke();
      List<Method> complexMethods = Arrays.asList(complexObj.getClass().getMethods());
      for(Method m : complexMethods){
         if(namingConventions.get(m.getReturnType().toString()) != null)
            s += processPrimitive(namingConventions,m.getReturnType().toString(),m.getName().substring(3,4).toLowerCase() + m.getName().substring(4),1);
         else{
            s += "\nmessage " + (complexObj.getClass() + "").substring(6) + " {\n"; 
            Object r = m.invoke(complexObj);
            processComplex(namingConventions,complexMethods,r,s,complexMethods.indexOf(m));
         }
      }
      return s;
   }
   
   private static void swap(List<Method> methods,int i, int j){
      Method temp = methods.get(i);
      methods.set(i,methods.get(j));
      methods.set(j,temp);
   }
}
