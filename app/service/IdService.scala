package service

class IdService {
  var current = 0

  def nextId(): Int = {
    current += 1
    current
  }
}
