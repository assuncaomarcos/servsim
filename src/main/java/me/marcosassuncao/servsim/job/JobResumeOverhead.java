package me.marcosassuncao.servsim.job;

/**
 * This interface defines the methods that an object should implement
 * to compute the resume overhead of jobs.
 * 
 * @author Marcos Dias de Assuncao
 */

public interface JobResumeOverhead {
	
	/**
	 * Gets the resume overhead for a given job
	 * @param tk the ticket for which the overhead must be computed
	 * @return the overhead
	 */
	long getResumeOverhead(Job tk);
	
	/**
	 * Sets additional parameters used by the class
	 * @param key the name of the parameter
	 * @param value the parameter value
	 */
	void setParam(String key, String value);
	
}
