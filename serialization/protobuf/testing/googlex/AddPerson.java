import com.example.tutorial.AddressBookProtos.AddressBook;
import com.example.tutorial.AddressBookProtos.Person;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import com.google.protobuf.*;

public class AddPerson{
   //construct a Person using Person.Builder and builder.build() with user input
   public static Person PromptForAddress(BufferedReader stdin,PrintStream stdout) throws IOException{
      Person.Builder person = Person.newBuilder();
      
      stdout.print("Enter person ID: ");
      person.setId(Integer.valueOf(stdin.readLine()));
      
      stdout.print("Enter name: ");
      person.setName(stdin.readLine());
       
      stdout.print("Enter email address: ");
      person.setEmail(stdin.readLine());
       
      stdout.print("Enter phone number: ");
      String number = stdin.readLine();
      Person.PhoneNumber.Builder phoneNumber = Person.PhoneNumber.newBuilder().setNumber(number);
      
      stdout.print("Mobile,home,or work?: ");
      String type = stdin.readLine();
      if (type.equals("mobile")) 
         phoneNumber.setType(Person.PhoneType.MOBILE);
      else if (type.equals("home")) 
         phoneNumber.setType(Person.PhoneType.HOME);
      else if (type.equals("work")) 
         phoneNumber.setType(Person.PhoneType.WORK);
      else 
         stdout.println("Unknown phone type");
      
      person.addPhones(phoneNumber);
      
      Person constructedPerson = person.build();
      return constructedPerson;
   }
   
   //Reads entire address book from file, adds the person constructed above, writes out to same file
   public static void main(String [] args) throws Exception{
      args = new String[1];
      Scanner in = new Scanner(System.in);
      System.out.print("Input data: ");
      args[0] = in.next();
      if (args.length != 1) {
         System.err.println("Usage:  AddPerson ADDRESS_BOOK_FILE");
         System.exit(-1);
      }
    
      AddressBook.Builder addressBook = AddressBook.newBuilder();
    
    //read existing address book
      try{
         addressBook.mergeFrom(new FileInputStream(args[0]));
      }
      catch(FileNotFoundException e){
         System.out.println(args[0] + ": File not found.  Creating a new file.");
      }
      //add address
      Person p = PromptForAddress(new BufferedReader(new InputStreamReader(System.in)),System.out);
      addressBook.addPeople(p);
      
      byte[] vaporized = p.toByteArray();
      for(byte b : vaporized){
         System.out.print(b+", ");
      }
      Person devaporized = p.parseFrom(vaporized);
      if(p.equals(devaporized))
         System.out.print("Devaporization successful!");
      else
         System.out.print("Devaporization unsuccessful!");
         
      FileOutputStream output = new FileOutputStream(args[0]);//par = filename
      addressBook.build().writeTo(output);
      output.close();
   }
}