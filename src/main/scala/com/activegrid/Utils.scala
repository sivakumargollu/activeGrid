package com.activegrid

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global;


class Utils {

  def persistSoftwareDetails(software: Software): Future[Software] = Future {
    //Need to write neo4j code here
    Software("1","Ubuntu","Linux") // Fake response

  }

}