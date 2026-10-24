package br.com.petrickfaria.jetpack.player;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class JetpackTerrainTransitionTest {
 @Test void smallDropsConvergeWithoutOvershoot() {
  for(double drop:new double[]{0.1,1,2,5}) {
   double remaining=drop,velocity=0;
   for(int tick=0;tick<200 && remaining>1E-8;tick++) {
    velocity=JetpackFlight.smoothTerrainCorrectionVelocity(velocity,0.375,remaining,0.06);
    assertTrue(velocity<=0 && -velocity<=remaining+1E-9);
    remaining+=velocity;
   }
   assertEquals(0,remaining,1E-8);
  }
 }
}
