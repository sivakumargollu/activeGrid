package com.imaginea.activegrid.core.models

import java.util.Base64

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.imaginea.Main

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, duration}


/**
  * Created by sivag on 22/12/16.
  */
object AdminManagerImpl {

  implicit val system = Main.system
  implicit val materializer = Main.materializer
  implicit val executionContext = Main.executionContext

  /**
    * @param baseUri
    * @param siteId
    * @param instanceId
    * @param resouce
    * @return ResouceUtilization
    *
    */
  //scalastyle:off cyclomatic.complexity method.length
  //todo 'sendDataAsJson' implementation
  def fetchMetricData(baseUri: String, siteId: Long, instanceId: String, resouce: String): List[ResouceUtilization] = {
    val emptyResponse = List(ResouceUtilization("target", List.empty[DataPoint]))
    // Getting application management server.
    val serverDetails = APMServerDetails.getAPMServerByInstance(siteId, instanceId)
    serverDetails.map {
      sdetails =>
        val instancenName = Instance.instanceBySiteAndInstanceID(siteId, instanceId).map(i => i.name).getOrElse("NOT PROVIDED")
        val providerType = sdetails.provider
        providerType match {

          case NEWRELIC =>
            val plugIn = PluginManager.getPluginByName("apm-newrlic")
            plugIn.map {
              pi =>
                val url = baseUri.concat("/plugins/{plugin}/servers/{serverId}/metrics".replace("{plugin}",
                  pi.name).replace("{serverId}", sdetails.id.toString()))
                val prop = Map("resouce" -> resouce, "instance" -> instancenName)
                val authStrategy = AppSettings.getAuthSettingsFor("auth.strategy")
                val headers = getHeadersAsPerAuthStrategy(authStrategy)
                val queryParams = Map.empty[String, String]
                val merticData = HttpClient.getData(url, headers, queryParams)
                convertToResouceUtilizationType(merticData.getOrElse(""))
            }.getOrElse(emptyResponse)
          case GRAPHITE =>
            val plugIn = PluginManager.getPluginByName("apm-graphite")
            plugIn.map {
              pi =>
                val url = baseUri.concat("/plugins/{plugin}/metrics".replace("{plugin}", pi.name))
                val query: APMQuery = APMQuery("carbon.agents.ip-10-191-186-149-a.cpuUsage", "-1h", "until", "json", sdetails.serverUrl)
                val headers = getHeadersAsPerAuthStrategy("anonymous")
                val queryParams = Map.empty[String, String]
                val metricData = HttpClient.sendDataAsJson("put", url, headers, queryParams, query).getOrElse("")
                convertToResouceUtilizationType(metricData)
            }.getOrElse(emptyResponse)
          case _ => emptyResponse
        }
    }.getOrElse(emptyResponse)
  }

  /**
    * Application metrics will describe the running status of application.
    *
    * @param baseUri
    * @param aPMServerDetails
    * @return
    *
    */
  //todo getData and convertResposneToType methods implementation
  def fetchApplicationMetrics(baseUri: String, aPMServerDetails: APMServerDetails): List[Application] = {
    val emptyResponse = List.empty[Application]
    aPMServerDetails.provider match {
      case NEWRELIC => PluginManager.getPluginByName("apm-newrilic").map {
        plugIn =>
          val queryParams = Map.empty[String, String]
          val headers = getHeadersAsPerAuthStrategy("anonymous")
          val url = baseUri.concat("/plugins/{plugin}/servers/{serverId}/applications".replace("{plugin}", plugIn.name).replace("{serverId}", aPMServerDetails.id.getOrElse("0L").toString()))
          val response = HttpClient.getData(url, headers, queryParams).getOrElse("")
          //todo logic that extract data from response and covnert data into application beans.
          convertApplicationType(response)
      }.getOrElse(emptyResponse)
      case GRAPHITE => // No procedure implemented.
        val response = "PROCEDURE NOT YET DEFINED"
        convertApplicationType(response)
      case _ =>
        emptyResponse
    }
  }

  /**
    * Connfigure the headers required for basic authorization.
    *
    * @param authStrategy
    * @return
    */

  def getHeadersAsPerAuthStrategy(authStrategy: String): Map[String, String] = {
    val emptyHeaders = Map.empty[String, String]
    authStrategy match {
      case "anonymous" =>
        val apps = "apiuser:password"
        val ciper: String = "Basic" + Base64.getEncoder.encode(apps.getBytes()).toString
        val headers = Map("Authorization" -> ciper)
        headers
      case "someotherstrategy" =>
        // configuring headers
        emptyHeaders
      case _ =>
        emptyHeaders
    }
  }

  /**
    * Unmarshall given response to ResouceUtilization type
    * @param response
    * @return List of ResourceUtilization objects, Empty list if the response is not valid    
    */
  def convertToResouceUtilizationType(response: String): List[ResouceUtilization] = {
    implicit val resouceUtilization = Main.resouceUtilizationFormat
    if (response.length > 0) {
      val mayBeResult = Unmarshal[String](response).to[ResouceUtilization]
      List(Await.result(mayBeResult, Duration(10, duration.MICROSECONDS)))
    } else {
      List.empty[ResouceUtilization]
    }
  }

  /**
    * Unmarshlls response type to Application type
    * @param response
    * @return List of application, Empty List if response is empty or invalid
    */
  def convertApplicationType(response: String): List[Application] = {
    implicit val applicationFormat = Main.applicationFormat
    val emptyResponse = List.empty[Application]
    if (response.length > 0) {
      val mayBeApp = Unmarshal(response).to[Application]
      List(Await.result(mayBeApp, Duration(10, duration.MICROSECONDS)))
    } else {
      emptyResponse
    }
  }
}
