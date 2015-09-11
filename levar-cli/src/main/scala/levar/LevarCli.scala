package levar

import org.rogach.scallop._
import org.rogach.scallop.exceptions._
import org.joda.time.format.DateTimeFormat
import com.typesafe.config.ConfigFactory
import levar.client._
import levar.io._
import levar.util._
import scala.io.StdIn.readLine
import scala.concurrent.Await
import scala.concurrent.duration._

/** CLI runner class */
object LevarCli {

  implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext

  def main(argv: Array[String]) {

    // Settings -- app defaults and environment variables
    val conf = ConfigFactory.load

    /// CLI args
    val args = new ScallopConf(argv) {

      version(s"LeVar CLI (client v${levar.build.BuildInfo.version})")

      banner("""
         |Command-line interface to a LeVar evaluation dataset service
         |
         |Usage: levar-cli [Options] [Subcommmand] [Subcommand params]
         |
         |Options:""".stripMargin)

      val configCmd = new Subcommand("config") {
        val url = opt[String](
          "url",
          noshort = true,
          descr = "URL of your LeVar server")

        val username = opt[String](
          "username",
          descr = "Your username")

        val password = opt[String](
          "password",
          descr = "Your password")

        val org = opt[String](
          "org",
          descr = "Your default organization"
        )
      }

      val datasetsCmd = new Subcommand("datasets") {
        val org = trailArg[String](required = false, descr = "Your organization")
      }
    }

    args.subcommand match {

      case Some(args.configCmd) => {
        ClientConfigIo.loadClientConfig match {
          case Some(cfg) => {
            var setAnything = false
            var newcfg = cfg
            for (url <- args.configCmd.url) {
              var newUrl = url
              while (!validURL(newUrl)) {
                println("Please use valid URL")
                newUrl = readLine("URL [${cfg.url}]: ").trim
              }
              if (newUrl.nonEmpty) {
                newcfg = newcfg.copy(url = url)
              }
              setAnything = true
            }
            for (username <- args.configCmd.username) {
              newcfg = newcfg.copy(username = username)
              setAnything = true
            }
            for (password <- args.configCmd.password) {
              newcfg = newcfg.copy(password = password)
              setAnything = true
            }
            for (org <- args.configCmd.org) {
              newcfg = newcfg.copy(org = org)
              setAnything = true
            }

            if (!setAnything) {
              var haveValidUrl = false
              while (!haveValidUrl) {
                val newUrl = readLine(s"URL [${cfg.url}]: ").trim
                if (newUrl.nonEmpty) {
                  if (validURL(newUrl)) {
                    haveValidUrl = true
                    setAnything = true
                  } else {
                    println("Please use valid URL")
                  }
                } else {
                  haveValidUrl = true
                }
              }

              val newUser = readLine(s"Username [${cfg.username}]: ").trim
              if (newUser.nonEmpty) {
                newcfg = newcfg.copy(username = newUser)
                setAnything = true
              }

              var haveValidPass = false
              while (!haveValidPass) {
                print(s"Password [${cfg.password.map(x => '*')}]: ")
                val standardIn = System.console()
                val newPass = new String(standardIn.readPassword())
                if (newPass.nonEmpty) {
                  print("Verify password: ")
                  val newPass2 = new String(standardIn.readPassword())
                  if (newPass != newPass2) {
                    println("Password does not match")
                  } else if (newPass.nonEmpty) {
                    newcfg = newcfg.copy(password = newPass)
                    setAnything = true
                    haveValidPass = true
                  }
                } else {
                  haveValidPass = true
                }
              }

              var haveValidOrg = false
              while (!haveValidOrg) {
                val newOrg = readLine(s"Organization [${cfg.org}]: ").trim
                if (newOrg.nonEmpty) {
                  if (validOrgName(newOrg)) {
                    newcfg = newcfg.copy(org = newOrg)
                    haveValidOrg = true
                    setAnything = true
                  }
                } else {
                  haveValidOrg = true
                }
              }
            }

            if (setAnything) {
              ClientConfigIo.saveClientConfig(newcfg)
            }
          }

          case None => {
            var url = args.configCmd.url.get.getOrElse("")
            while (url.isEmpty || !validURL(url)) {
              if (!validURL(url)) {
                println("Please use valid URL")
              }
              url = readLine("URL: ").trim
            }

            var username = args.configCmd.username.get.getOrElse("")
            while (username.isEmpty) {
              username = readLine("Username: ").trim
            }

            var password = args.configCmd.password.get.getOrElse("")
            val standardIn = System.console()
            while (password.isEmpty) {
              print("Password: ")
              password = new String(standardIn.readPassword).trim
              print("Verify password: ")
              val passwordVerify = new String(standardIn.readPassword).trim
              if (password != passwordVerify) {
                println("Password does not match")
                password = ""
              }
            }

            var org = args.configCmd.org.get.getOrElse("")
            while (org.isEmpty) {
              org = readLine("Organization: ").trim
            }

            val newcfg = ClientConfig(username, password, url, org)
            ClientConfigIo.saveClientConfig(newcfg)
          }
        }
      }

      case Some(args.datasetsCmd) => {
        try {
          val client = ClientConfigIo.loadClient
          val org = args.datasetsCmd.org.get.getOrElse(client.config.org)
          val datasetRS = Await.result(client.searchDatasets(org), 10 seconds)
          if (datasetRS.nonEmpty) {
            println(s"Datasets for $org")
            println(
              """|Dataset                    | Updated    | Type       |    Items
                 |---------------------------|------------|------------|----------""".stripMargin)
            for (dataset <- datasetRS) {
              val displayName = if (dataset.id.size > 26) {
                dataset.id.take(23) + "..."
              } else {
                dataset.id + (" " * (26 - dataset.id.size))
              }
              val date = dataset.updatedAt match {
                case Some(d) => DateTimeFormat.forPattern("yyyy-MM-dd").print(d)
                case None => " " * 10
              }
              val dtype = {
                val t = dataset.dtype match {
                  case Dataset.ClassificationType => "classify"
                  case Dataset.RegressionType => "regression"
                }
                t + (" " * (10 - t.size))
              }
              val n = dataset.size.getOrElse(0)
              println(f"$displayName | $date | $dtype | $n%8d")
            }
          } else {
            println(s"No datasets for $org")
          }
        } catch {
          case _: MissingClientConfig => {
            Console.err.println("Missing cofig -- run `levar config` to set up")
            sys.exit(1)
          }
        }
      }

      case _ => println("You did not supply an argument -- try 'levar-cli datasets'")
    }

    sys.exit
  }
}
