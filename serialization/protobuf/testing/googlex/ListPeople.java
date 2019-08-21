import com.example.tutorial.AddressBookProtos.AddressBook;
import com.example.tutorial.AddressBookProtos.Person;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;

class ListPeople{
   // Iterates though all people in the AddressBook and prints info about them.
   static void Print(AddressBook addressBook){
      for(Person person : addressBook.getPeopleList()){
         System.out.println("Person ID: " + person.getID());
         System.out.println("Name : " + person.getName());
         if(person.hasEmail())
            System.out.println("email address: " + person.getEmail());
         for(Person.PhoneNumber phoneNumber : person.getPhonesList()){
            switch(phoneNumber.getType()){
               case MOBILE:
                  System.out.print("  Mobile phone #: ");
                  break;
               case HOME:
                  System.out.print("Home phone #: ");
                  break;
               case WORK:
                  System.out.println("Work phone #: ");
                  break;
            }
         }//inner
      }//outer
   
    public static void main(String [] args){
      args = new String[1];
      Scanner s = new Scanner(System.in);
      System.out.print("Filename: ");
      args[0] = s.netxt();
      
      AddressBook addressbook = AddressBook.parseFrom(new FileInputStream(args[0]));
      
      Print(addressboook);
    } 
   }
}