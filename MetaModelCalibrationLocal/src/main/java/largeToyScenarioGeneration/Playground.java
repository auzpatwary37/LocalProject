package largeToyScenarioGeneration;

//import com.numericalmethod.suanshu.algebra.linear.vector.doubles.Vector;
//import com.numericalmethod.suanshu.algebra.linear.vector.doubles.dense.DenseVector;
//import com.numericalmethod.suanshu.analysis.function.rn2r1.RealScalarFunction;
//import com.numericalmethod.suanshu.optimization.multivariate.unconstrained.c2.NelderMeadMinimizer;
//import com.numericalmethod.suanshu.optimization.problem.C2OptimProblem;
//import com.numericalmethod.suanshu.optimization.problem.C2OptimProblemImpl;

import ust.hk.praisehk.metamodelcalibration.calibrator.ObjectiveCalculator;
import ust.hk.praisehk.metamodelcalibration.measurements.Measurements;
import ust.hk.praisehk.metamodelcalibration.measurements.MeasurementsReader;

public class Playground {
public static void main(String[] args) {
//	Measurements m1=new MeasurementsReader().readMeasurements("toyScenarioLarge/fabricatedCountNew_average1.xml");
//	Measurements m2=new MeasurementsReader().readMeasurements("toyScenarioLarge/fabricatedCountNew_average2.xml");
//	double d=ObjectiveCalculator.calcObjective(m1, m2, ObjectiveCalculator.TypeMeasurementAndTimeSpecific);
//	System.out.println(d);
//	System.out.println(Math.sqrt(d/475));
	Measurements m1=new MeasurementsReader().readMeasurements("toyScenarioLarge/Calibration/simMeasurement0.xml");
	m1.writeCSVMeasurements("toyScenarioLarge/Calibration/simMeasurement0_230210Laptop1.csv");
//	double[] lamda=new double[] {83.8200929965927,93.9366194814093,86.5268861078996};
//	double[] ysim=new double[] {103,105,107};
//	double[][] theta=new double[][] {{-200,-200},{-200.055508572691,-199.613005281576},{-200.764263528660,-196.814626207834}};
//	int currentParam=2;
//	RealScalarFunction f=new objectiveFunction(lamda,ysim,theta,currentParam);
//	NelderMeadMinimizer n=new  NelderMeadMinimizer(.0001, 1500);
//	C2OptimProblem problem=new C2OptimProblemImpl(f); 
//	NelderMeadMinimizer.Solution s=n.solve(problem);
//	double[] meta=new double[] {1,1,1,1};
//	int dim=meta.length;
//	DenseVector[] initialSimplex=new DenseVector[dim+1];
//	double[] dd=new double[meta.length];
//	for(int i=0;i<=meta.length;i++) {
//		if(i>0) {
//			dd[i-1]=meta[i-1];
//		}
//		initialSimplex[i]=new DenseVector(dd);
//	}
//	s.setInitials(initialSimplex);
//	System.out.println(s.minimizer());
//	System.out.println(s.minimum());
}
}
//class objectiveFunction implements RealScalarFunction{
//
//	private double[] lamda;
//	private double[] ysim;
//	private double[][] theta;
//	private double[] metaParam;
//	private int currentParamIterNo;
//	
//	public objectiveFunction(double[]lamda,double[]ysim,double[][]theta,int currentParamIterNo) {
//		this.lamda=lamda;
//		this.ysim=ysim;
//		this.theta=theta;
//		this.currentParamIterNo=currentParamIterNo;
//		this.metaParam=new double[this.theta[0].length+2];
//	}
//	
//	@Override
//	public int dimensionOfDomain() {
//		
//		return this.metaParam.length;
//	}
//
//	@Override
//	public int dimensionOfRange() {
//		
//		return 1;
//	}
//
//	@Override
//	public Double evaluate(Vector x) {
//		this.metaParam=x.toArray();
//		double obj=0;
//		for(int i=0; i<this.lamda.length;i++) {
//			double weight=1/(1+this.calcDistance(this.theta[this.currentParamIterNo], this.theta[i]));
//			obj+=weight*Math.pow(ysim[i]-this.calculateMetaModel(this.lamda[i], this.theta[i]),2);
//		}
//		for(int i=0;i<this.metaParam.length;i++) {
//			obj+=Math.pow(this.metaParam[i],2);
//		}
//		return obj;
//	}
//	
//	private double calculateMetaModel(double lamda, double[] theta) {
//		
//		double m=this.metaParam[0]+this.metaParam[1]*lamda;
//		for(int i=2;i<this.metaParam.length;i++) {
//			m+=theta[i-2]*this.metaParam[i];
//		}
//		
//		return m;
//	}
//	
//	public double calcDistance(double[] theta1,double[]theta2) {
//		if(theta1.length!=theta2.length) {
//			throw new IllegalArgumentException("Dimension mismatch!!!");		
//			}
//		double d=0;
//		for(int i=0;i<theta1.length;i++) {
//			d+=Math.pow((theta1[i]-theta2[i]),2);
//		}
//		d=Math.sqrt(d);
//		return d;
//	}
//}


