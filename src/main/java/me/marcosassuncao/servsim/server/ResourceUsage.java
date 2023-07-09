package me.marcosassuncao.servsim.server;

/**
 * Class used to return the results of a query to check
 * the resource usage of a given resource profile.
 *
 * @author Marcos Dias de Assuncao
 *
 * @param time         the time associated with the object
 * @param numResources the number of resources in use at <code>time</code>
 */
public record ResourceUsage(long time, int numResources) {

}
