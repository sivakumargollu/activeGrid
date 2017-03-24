package com.imaginea.activegrid.core.models

import scala.collection.mutable
import scala.concurrent.Future

import java.util.concurrent.{ScheduledExecutorService, TimeUnit}

import com.imaginea.Main
import com.imaginea.activegrid.core.utils.ActiveGridUtils
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.concurrent.duration.Duration

/**
  * Created by nagulmeeras on 13/01/17.
  */
/*class AnsibleWorkflowProcessor {
  val workflowExecutors = mutable.HashMap.empty[Long, Future[Unit]]

  def stopWorkflow(workflow: Workflow): Boolean = {
    workflow.id.exists(id => workflowExecutors.remove(id).isDefined)
  }
}*/
object AnsibleWorkflowProcessor extends WorkflowProcessor{


  val workflowExecutors = Map.empty[Long, ScheduledExecutorService]
  val logger = Logger(LoggerFactory.getLogger(AnsibleWorkflowProcessor.getClass.getName))

    def executeWorkflow(workflowContext: WorkflowContext, async: Boolean): Unit = {
    //scalastyle:off magic.number
    implicit val system = Main.system
    implicit val materializer = Main.materializer
    implicit val executionContext = ActiveGridUtils.getExecutionContextByService("workflow")
    val workflow = workflowContext.workflow
    val playBookRunner = new AnsiblePlayBookRunner(workflowContext)
    if (async) {
      if (CurrentRunningWorkflows.size > WorkflowConstants.maxParlellWorkflows) {
        logger.error("Too many parlell workflows trigger, Max parllel works ares " + WorkflowConstants.maxParlellWorkflows)
        //todo code to break execution
      }
      val workflowId = workflow.id.getOrElse(0L)
      CurrentRunningWorkflows.get(workflowId).map {
        node => logger.info("Workflow [" + workflow.name + "] is currently running, Please try after some time.")
        //todo code to break execution
      }
      val result  = Future { playBookRunner.executePlayBook()}
    }
    else {
      playBookRunner.executePlayBook()
    }
  }
  def stopWorkflow(workflow: Workflow): Unit = {
    workflow.id.foreach {
      id => CurrentRunningWorkflows.remove(id)
    }
  }

}
