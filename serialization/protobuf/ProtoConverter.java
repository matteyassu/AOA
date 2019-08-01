import java.io.*;
import java.util.*;
import java.lang.reflect.*;
/*
convert object to protobuffer format and write to txt file. Then stick txt file (cbor?) into bytebuffer somehow, then transfer over to CAT
On CAT end, re-inflate txt to object and manipulate/display 
*/

/*
TODO: indentation
      change to .proto file
      automate protoc
*/
public class ProtoConverter{

   public static void main(String [] args)throws Exception{
      Astronaut a = new Astronaut();
      Rocket r = new Rocket(a,"FalconHeavy",5,10000,3);
      
      Map<String,String> h = new HashMap<>();
      h.put("int","int32");
      h.put("Integer","int32");
      h.put("class java.lang.Integer","int32");
      h.put("String","String");
      h.put("String","String");
      h.put("class java.lang.String","String");
      h.put("boolean","bool");
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
      String s = "syntax = \"proto" + version + "\";\r\n\r\npackage " + pkgName + ";\r\n";
      if(java_package)
         s += "\r\n\r\noption java_package = \"" + java_pkg + "\";\r\n";
      if(outer_class_name)
         s+= "\r\noption java_outer_classname = \"" + outerName + "\";\r\n";
      String output = generateAttributes(namingConventions,o,0);
      s += output + "\r\n}";
      System.out.println(s);
      writer.write(s);
      writer.close();
      return f;
   }
   private static String generateAttributes(Map<String,String> namingConventions, Object o,int numIndent)throws Exception{
      String attributes = addIndent(numIndent) + "message " + (o.getClass() + "").substring(6) + " {\r\n";
      numIndent++;
      Field [] fields = o.getClass().getDeclaredFields();
      Object p = null; 
      for(int i = 0; i < fields.length; i++) {
         String fieldType = fields[i].getType().toString();
         String fieldName = fields[i].getName();
         if(namingConventions.get(fieldType) == null) {
         	//swap to ensure complex objects are enumerated last
            boolean swapped = false;
            for(int j = fields.length - 1; j > i; j--) {
               if(namingConventions.get(fields[j].getType().toString()) != null) {
                  Field temp = fields[j];
                  fields[j] = fields[i];
                  fields[i] = temp;
                  swapped = true;
                  break;
               }
            }
            if(swapped) {
               fieldType = fields[i].getType().toString();
               fieldName = fields[i].getName();
               attributes += addIndent(numIndent) + namingConventions.get(fieldType) + " " + fieldName + " = " + i + ";\r\n";
               continue;
            }
            try {
               p = fields[i].get(o);
            }
            catch(IllegalAccessException e) {
               e.printStackTrace();
            }
            attributes += "\r\n" + generateAttributes(namingConventions,p,numIndent);
         }
         else
            attributes += addIndent(numIndent) + namingConventions.get(fieldType) + " " + fieldName + " = " + i + ";\r\n";
      }
   	// try {
   // 			writer.write(attributes);
   // 		}
   // 		catch(IOException e) {
   // 			e.printStackTrace();
   // 		}
      return attributes;
   } 
   private static String addIndent(int numIndent) {
      String indent = "";
      for(int i = 0; i < numIndent; i++) 
         indent += "  ";
      return indent;
   }
     //  Field [] fields = o.getClass().getDeclaredFields();
//       String attributes = "";
//       Object p = null;
//       for(int i = 0; i < fields.length; i++) {
// 			String fieldType = fields[i].getType().toString();
// 			String fieldName = fields[i].getName();
// 			if(namingConventions.get(fieldType) == null) {
// 				p = fields[i].get(o);
// 				//attributes += generateAttributes(p,attributes);
// 			}
// 		}
//       
//       int i = 0;
//       List<Method> methods = Arrays.asList(o.getClass().getMethods());
//       for(Method m : methods){
//          String methodName = m.getName(); 
//          if(methodName.substring(0,3).equals("get") && m.getParameterTypes().length == 0){
//             if(m.getName().equals("getClass"))
//                continue;
//                //gather components of method signature
//                //ex:engineType
//             String returnVar = methodName.substring(3,4).toLowerCase() + methodName.substring(4);
//                //ex:String
//             String returnType = m.getReturnType().toString();
//             boolean builtIn = false;
//             if(namingConventions.get(returnType) != null)
//                builtIn = true;
//             else if(returnType.contains(".")){
//                //if comes in class java.lang.String format
//                returnType = returnType.substring(returnType.lastIndexOf(".")+1);
//                if(namingConventions.get(returnType) != null)
//                   builtIn = true;
//             }
//                //primitive
//             if(builtIn)
//                s += processPrimitive(namingConventions,returnType,returnVar,i);
//                //complex
//             else{
//                int endIndex = methods.indexOf(m);
//                s += processComplex(namingConventions,methods,o,"",endIndex); 
//             }
//          }
//       }
//       return s;
   //}
   
   private static String processPrimitive(Map<String,String> namingConventions,String returnType,String returnVar,int i){
      return namingConventions.get(returnType) + " " + returnVar + " = " + i + ";\r\n";
   }
   
   // private static boolean builtIn(Map<String,String> namingConventions,String returnType){
//       
//    }
   private static String processComplex(Map<String,String> namingConventions,List<Method> methods,Object o,String s,int endIndex)throws Exception{
      //execute swapping algo and call processPrimitive() OR swapping not possible and execute processComplex()
      
      
      
      for(int i = methods.size()-1; i >= endIndex; i--){
         //if get(),0 param,and built in swap
         String name = methods.get(i).getName();
         if(methods.get(i).getName().substring(0,3).equals("get") && methods.get(i).getParameterTypes().length == 0){
            if(methods.get(i).getName().equals("getClass"))
               continue;
            String returnType = methods.get(i).getReturnType().toString();
            boolean builtIn = false;
            if(namingConventions.get(returnType) != null)
               builtIn = true;
            else if(returnType.contains(".")){
               //if comes in class java.lang.String format
               returnType = returnType.substring(returnType.lastIndexOf(".")+1);
               if(namingConventions.get(returnType) != null)
                  builtIn = true;
            }
            if(builtIn){
               swap(methods,endIndex,i);
               return processPrimitive(namingConventions,returnType,methods.get(endIndex).getName().substring(3,4).toLowerCase() + methods.get(endIndex).getName().substring(4),1);
            }
         }
      } 
       
      String className = methods.get(endIndex).getReturnType().toString().substring(6); 
      s += "\r\nmessage " + className + "{ \r\n";
      List<Method> complexMethods = Arrays.asList(methods.get(endIndex).getReturnType().getMethods());
      for(Method m : complexMethods){
         String name = m.getName();
         if(m.getName().substring(0,3).equals("get") && m.getParameterTypes().length == 0){
            if(m.getName().equals("getClass"))
               continue;
            String returnType = m.getReturnType().toString();
            boolean builtIn = false;
            if(namingConventions.get(returnType) != null)
               builtIn = true;
            else if(returnType.contains(".")){
               //if comes in class java.lang.String format
               returnType = returnType.substring(returnType.lastIndexOf(".")+1);
               if(namingConventions.get(returnType) != null)
                  builtIn = true;
            }
            if(builtIn){
               String returnVar = m.getName();
               s += processPrimitive(namingConventions,returnType,m.getName().substring(3,4).toLowerCase() + m.getName().substring(4),1);
            }      
            else{
               Object r = complexMethods.get(endIndex).getReturnType();
               s += "\r\n\r\nmessage " + (r.getClass() + "").substring(6) + " {\r\n"; 
               processComplex(namingConventions,complexMethods,r,s,complexMethods.indexOf(m));
            }
         }
      }
      return s + "}\r\n";
   }
   
   private static void swap(List<Method> methods,int i, int j){
      Method temp = methods.get(i);
      methods.set(i,methods.get(j));
      methods.set(j,temp);
   }
}