/*
 * Notes: Check for String.equals("") instead of using booleans in skeleton generation?
 * 		  Added features:
 * 			required vs optional variables
 * 			repeated variables
 *        Use protoc compiler to generate binary
 * 
   INFLATION COMMAND(from within directory that .java file is located in) protoc --java_out=C:\Users\meyassu\workspace\Android\AOA\serialization\protobuf rocket.proto
 */
import java.io.*;
import java.util.*;
import java.lang.reflect.*;
public class ProtoConverter {
   
   public static void main(String [] args) throws IOException{
      Astronaut a = new Astronaut();
      Rocket r = new Rocket(a,"FalconHeavy",5,10000,3);
      ProtoConverter proto = new ProtoConverter(r);
      String skeleton = proto.generateSkeleton(2,"NASA",true,"com.example.proto",false,"");
      String attributes = proto.generateAttributes(r,0);
      System.out.println(skeleton + attributes);
      FileWriter writer = proto.getWriter();
      try {
         writer.write(skeleton + attributes);
      }
      catch(IOException i) {
         i.printStackTrace();
      }
      catch(Exception e){
         e.printStackTrace();
      }
      finally{
         try{
            writer.close();
          }
          catch(IOException i){
            i.printStackTrace();
          }
      }
      proto.compile();
   }
   
   private Map<String,String> typeConventions;
   private Object o;
   private File protoFile;
   private FileWriter writer;
   
   //constructor: map,object,file,writer initialization 
   public ProtoConverter(Object o) {
      typeConventions = new HashMap<String,String>();
      typeConventions.put("Integer","int32");
      typeConventions.put("class java.lang.Integer","int32");
      typeConventions.put("String","string");
      typeConventions.put("String","string");
      typeConventions.put("class java.lang.String","string");
      typeConventions.put("boolean","bool");
      typeConventions.put("int","int32");
   	
      this.o = o;
   	
      generateFile();
   	
      generateFileWriter();
   }
   //getters
   public Map<String,String> getTypeConventions(){
      return typeConventions;
   }
   public Object getObject(){
      return o;
   }
   public FileWriter getWriter(){
      return writer;
   }
   private boolean generateFile() {
      Scanner scan = new Scanner(System.in);
      boolean createFile = false;
   	//multiple chances for filename input
      while(!createFile) {
         System.out.print("Please enter a filename (with .proto extenstion): ");
         String filename = scan.next(); 
         try {
            protoFile = new File(filename);
            createFile = protoFile.createNewFile();
            createFile = true;
         }
         catch(IOException i) {
            i.printStackTrace();
         }
      }
      scan.close();
      return createFile;
   }
   private boolean generateFileWriter() {
      try {
         writer = new FileWriter(protoFile);
         return true;
      }
      catch(IOException i) {
         i.printStackTrace();
         return false;
      }
   }
   public String generateSkeleton(int version,String pkgName,boolean javaPkg,String javaPkgName,boolean outerClass,String outerClassName) {
      String doubleSpace = "\r\n\r\n";
      String skeleton = "syntax = \"proto" + version + "\";" + doubleSpace;
   	
   	//packages
      if(!javaPkg)
         javaPkgName = pkgName;
      skeleton += "package " + pkgName + ";" + doubleSpace;
      skeleton += "option java_package = \"" + javaPkgName + "\";\r\n";
   	
   	//outer_class_name
      if(!outerClass)
         outerClassName = this.getClass().getSimpleName();
      skeleton += "option java_outer_classname = \"" + outerClassName + "\";" + doubleSpace;
      
      return skeleton;
   }
	
	//recursive solution (n^n complexity if all fields are complex types; not a result of algorithmic design but inherent to problem i.e unavoidable)
   public String generateAttributes(Object o,int numIndent) {
      String attributes = "message " + (o.getClass() + "").substring(6) + " {\r\n";
      numIndent++;
      Field [] fields = o.getClass().getDeclaredFields();
      Object p = null; 
      for(int i = 0; i < fields.length; i++) {
         String fieldType = fields[i].getType().toString();
         String fieldName = fields[i].getName();
         if(typeConventions.get(fieldType) == null) {
         	//swap to ensure complex objects are enumerated last
            boolean swapped = false;
            for(int j = fields.length - 1; j > i; j--) {
               if(typeConventions.get(fields[j].getType().toString()) != null) {
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
               attributes += addIndent(numIndent) + "optional " + typeConventions.get(fieldType) + " " + fieldName + " = " + (i+1) + ";\r\n";
               continue;
            }
            try {
               p = fields[i].get(o);
            }
            catch(IllegalAccessException e) {
               e.printStackTrace();
            }
            //complex object
            attributes += addIndent(numIndent) + generateAttributes(p,numIndent) + addIndent(numIndent);
            attributes += addIndent(--numIndent) + "\r\n}";
         }
         //simple object
         else{
            attributes += addIndent(numIndent) + "optional " + typeConventions.get(fieldType) + " " + fieldName + " = " + (i+1) + ";\r\n";
            if(i == fields.length -1)
               attributes += addIndent(--numIndent) + "}";
            } 
      }
      return attributes;
   }
   private String addIndent(int numIndent) {
      String indent = "";
      for(int i = 0; i < numIndent; i++)
         indent += "  ";
      return indent;
   }
   
   public boolean compile() throws IOException{
      //run cmd commands using Runtime(interfere/use environment/os applications) and Process(actually executes process)
      //.exec() returns this Process
      
      //insert commands
      Runtime.getRuntime().exec("cmd /c protoc --java_out=C:\\Users\\meyassu\\workspace\\Android\\AOA\\serialization\\protobuf " + protoFile.getName());
      return true;
   }
	
		
}
