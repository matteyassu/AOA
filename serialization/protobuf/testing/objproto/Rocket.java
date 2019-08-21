public class Rocket{
   
   Astronaut astro;
   String engineType;
   int engineVersion;
   int mass;
   int terminalVelocity;
   
   public Rocket(Astronaut astro, String engineType,int engineVersion,int mass,int terminalVelocity){
      this.engineType = engineType;
      this.engineVersion = engineVersion;
      this.mass = mass;
      this.terminalVelocity = terminalVelocity;
      if(manned())
         this.astro = astro;
      else
         astro = null;
   }
   
   public boolean manned(){
      if(terminalVelocity > 1000)
         return false;
      else
         return true;
   }
   
   public Astronaut getAstronaut(){return astro;}
   public String getEngineType(){return engineType;}
   public int getEngineVersion(){return engineVersion;}
   public int getMass(){return mass;}
   public int getTerminalVelocity(){return terminalVelocity;}
   
}