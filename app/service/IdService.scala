package service

class IdService {
  var current = 0

  def nextId(): String = {
    current += 1
    "g" + current.toString
  }
}
