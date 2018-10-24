package largeToyScenarioGeneration;

import ust.hk.praisehk.metamodelcalibration.calibrator.ObjectiveCalculator;
import ust.hk.praisehk.metamodelcalibration.measurements.Measurements;
import ust.hk.praisehk.metamodelcalibration.measurements.MeasurementsReader;

public class Playground {
public static void main(String[] args) {
	Measurements m1=new MeasurementsReader().readMeasurements("toyScenarioLarge/fabricatedCount_0.xml");
	Measurements m2=new MeasurementsReader().readMeasurements("toyScenarioLarge/fabricatedCount_1.xml");
	double d=ObjectiveCalculator.calcObjective(m1, m2, ObjectiveCalculator.TypeMeasurementAndTimeSpecific);
	System.out.println(d);
	System.out.println(Math.sqrt(d)/475);
}
}
