package com.activegrid.models

import org.neo4j.graphdb.{Node, NotFoundException}
import org.slf4j.LoggerFactory

/**
  * Created by nagulmeeras on 14/10/16.
  */
case class Site(override val id: Option[Long],
                siteName: Option[String],
                groupBy: Option[String]) extends BaseEntity

object Site {
  val neo4JRepository = Neo4JRepository
  val logger = LoggerFactory.getLogger(getClass)
  val siteLabel = "Site"

  implicit class SiteImpl(site: Site) extends Neo4jRep[Site] {

    override def toNeo4jGraph: Node = {
      logger.debug(s"Executing $getClass ::toNeo4jGraph ")
      neo4JRepository.withTx {
        neo =>
          val node = neo4JRepository.createNode(siteLabel)(neo)
          if (!site.siteName.isEmpty) node.setProperty("siteName", site.siteName.get)
          if (!site.groupBy.isEmpty) node.setProperty("groupBy", site.groupBy.get)
          node
      }
    }

    override def fromNeo4jGraph(nodeId: Long): Option[Site] = {
      logger.debug(s"Executing $getClass ::fromNeo4jGraph ")
      Site.fromNeo4jGraph(nodeId)
    }
  }

  def fromNeo4jGraph(nodeId: Long): Option[Site] = {
    logger.debug(s"Executing $getClass ::fromNeo4jGraph ")
    neo4JRepository.withTx {
      neo =>
        try {
          val node = neo4JRepository.getNodeById(nodeId)(neo)
          if (neo4JRepository.hasLabel(node, siteLabel)) {
            val site = new Site(Some(nodeId),
              if (node.hasProperty("siteName")) Some(node.getProperty("siteName").asInstanceOf[String]) else None,
              if (node.hasProperty("groupBy")) Some(node.getProperty("groupBy").asInstanceOf[String]) else None)
            Some(site)
          } else {
            logger.warn(s"Node is not found with ID:$nodeId and Label : $siteLabel")
            None
          }
        } catch {
          case nfe: NotFoundException =>
            logger.warn(nfe.getMessage, nfe)
            None
        }
    }
  }
}

