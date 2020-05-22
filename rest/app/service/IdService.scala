package service

import java.util.UUID

class IdService {

  def nextId(): String = {
    UUID.randomUUID().toString
  }
}
