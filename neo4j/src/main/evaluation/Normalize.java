package evaluation;

public class Normalize {
	public  static  double minAvailability = 0.0000016;
	public static  double maxAvailability = 0.005;
	public  static  double minReliability =0.001408;
	public  static double maxReliability = 0.00935;
	public  static double minTime = 12858.46;
	public  static double maxTime = 23427.83;
	public  static double minCost = 44.14;
	public static  double maxCost = 48.11;
	private static double m_a = 0.25;
	private static double m_r = 0.25;
	private static double m_c = 0.25;
	private static double m_t = 0.25;
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("C1    ");
		double c1 = m_a*normalize(0.0000016,"A") + m_r*normalize(0.00935,"R") + m_c*normalize(48.11,"C") + m_t*normalize(23427.83,"T");
		System.out.println("C2    ");
		double c2 = m_a*normalize(0.005,"A") + m_r*normalize(0.001408,"R") + m_c*normalize(44.18,"C") + m_t*normalize(12858.46,"T");
		System.out.println(c1+"    "+c2);

	}
	private static double normalize(double total, String id) {
		if(id.equals("A")){
			if(maxAvailability-minAvailability == 0)
				return 1;
			else{
				System.out.println("A: "+(total - minAvailability)/(maxAvailability-minAvailability));
				return (total - minAvailability)/(maxAvailability-minAvailability);
			}
		}
		else if(id.equals("R")){
			if(maxReliability-minReliability == 0)
				return 1;
			else{
				System.out.println("R: "+(total - minReliability)/(maxReliability-minReliability));
				return (total - minReliability)/(maxReliability-minReliability);
			}
		}
		else if(id.equals("C")){
			if(maxCost-minCost == 0)
				return 1;
			else{
				System.out.println("C: "+(maxCost- total)/(maxCost-minCost));
				return (maxCost- total)/(maxCost-minCost);
			}
		}	
		else if(id.equals("T")){
			if(maxTime-minTime == 0)
				return 1;
			else{
				System.out.println("T: "+(maxTime- total)/(maxTime-minTime));
				return (maxTime- total)/(maxTime-minTime);
			}
		}	
		else return -1;
	}
}
