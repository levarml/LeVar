package db

import org.scalatest._

class DbImplSpec extends FlatSpec with BeforeAndAfterEach {

  override def beforeEach() {
    impl.setUp
  }

  override def afterEach() {
    impl.tearDown
  }

  "The DB" should "setUp and tearDown without breaking" in {
    info("seemed to work OK")
  }

  it should "run setUp twice without freaking out" in {
    impl.setUp
    info("seemed to work OK")
  }
}
