package com.imaginea.activegrid.core.models

/**
  * Created by sivag on 3/11/16.
  */
object SiteManagerImpl {

  def deleteIntanceFromSite(siteId: Long, instanceId: String): ExecutionStatus = {
    val siteNode = Site1.fromNeo4jGraph(siteId)
    siteNode match {
      case None => ExecutionStatus(false, s"Site ${siteId} not available")
      case Some(site) =>
        val instance = site.instances.map(instance => instance.id.toString == instanceId)

        //Removing instance  from groups list
        site.groupsList.map(instanceGroup => Neo4jRepository.deleteRelation(instanceId, instanceGroup, "instances"))
        //Need to remove from application.


        //Removing from site
        Neo4jRepository.deleteRelation(instanceId, site, "instances")
        ExecutionStatus(true, s"Instance ${instanceId} deleted from site ${siteId}")
    }
  }
}
