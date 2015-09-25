package levar

import org.rogach.scallop._
import org.rogach.scallop.exceptions._
import org.joda.time.format.DateTimeFormat
import com.typesafe.config.ConfigFactory
import levar.client._
import levar.io._
import levar.util._
import scala.io.Source
import scala.io.StdIn.readLine
import scala.concurrent.Await
import scala.concurrent.duration._
import breeze.linalg._
import breeze.stats._
import breeze.stats.DescriptiveStats._

/** CLI runner class */
object LevarCli {

  implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext

  val OrgThingPattern = """([-\w\.]+)/([-\w\.]+)""".r
  val ThingPattern = """([-\w\.]+)""".r

  def main(argv: Array[String]) {

    try {

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
          val uploadCmd = new Subcommand("upload") {
            val name = opt[String]("name", descr = "Name for the dataset (default = the filename)")
            val org = opt[String]("org", descr = "Upload dataset to a specific organization")
            val file = trailArg[String](required = true, descr = "TSV file for upload")
          }

          val viewCmd = new Subcommand("view") {
            val dataset = trailArg[String](required = true, descr = "Dataset to view")
          }

          val previewTsvCmd = new Subcommand("preview-tsv") {
            val name = opt[String]("name", descr = "Name for the dataset (default = the filename)")
            val file = trailArg[String](required = true, descr = "TSV file for upload")
          }

          val org = trailArg[String](required = false, descr = "Your organization")
        }
      }

      args.subcommands match {

        case List(args.configCmd) => {
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

        case List(args.datasetsCmd) => {
          try {
            val client = ClientConfigIo.loadClient
            val org = args.datasetsCmd.org.get.getOrElse(client.config.org)
            try {
              val datasetRS = Await.result(client.searchDatasets(org), 10 seconds)
              if (datasetRS.nonEmpty) {
                println(s"Datasets for $org")
                println(Format.datasetRStoString(datasetRS))
              } else {
                println(s"No datasets for $org")
              }
            } catch {
              case e: ConnectionError => {
                Console.err.println(s"Could not access datasets for $org")
                Console.err.println(e.getMessage)
                sys.exit(1)
              }
            }
          } catch {
            case _: MissingClientConfig => {
              Console.err.println("Missing cofig -- run `levar config` to set up")
              sys.exit(1)
            }
          }
        }

        case List(args.datasetsCmd, args.datasetsCmd.uploadCmd) => {
          try {
            val client = ClientConfigIo.loadClient
            val fileName = args.datasetsCmd.uploadCmd.file()
            val org = args.datasetsCmd.uploadCmd.org.get
            if (!(fileName.endsWith(".tsv") || fileName.endsWith(".txt"))) {
              throw new RuntimeException("Expected to see a TSV file (.tsv or .txt)")
            }
            val name = args.datasetsCmd.uploadCmd.name.get.getOrElse(fileName)
            val src = Source.fromFile(args.datasetsCmd.uploadCmd.file())
            try {
              val tabular = TsvDataset.fromSource(name, src)
              val dataset = tabular.asDataset
              Await.result(client.createDataset(dataset, org), 10 seconds)
              for (data <- tabular.data.grouped(250)) {
                val d = data.toSeq
                Await.result(client.uploadDatasetData(dataset.id, d, org), 10 seconds)
              }
              val saved = Await.result(client.getDataset(org, name), 10 seconds)
              println(Format.datasetToString(saved))
            } catch {
              case e: ConnectionError => {
                Console.err.println("Could not upload datset")
                Console.err.println(e.getMessage)
                sys.exit(1)
              }
            } finally {
              src.close()
            }
          } catch {
            case _: MissingClientConfig => {
              Console.err.println("Missing cofig -- run `levar config` to set up")
              sys.exit(1)
            }
          }
        }

        case List(args.datasetsCmd, args.datasetsCmd.viewCmd) => {
          val dsName = args.datasetsCmd.viewCmd.dataset()
          val client = ClientConfigIo.loadClient
          try {
            val dataset = dsName match {
              case OrgThingPattern(org, datasetId) =>
                Await.result(client.getDataset(Some(org), datasetId), 10 seconds)
              case ThingPattern(datasetId) =>
                Await.result(client.getDataset(None, datasetId), 10 seconds)
              case _ => {
                Console.err.println(s"Invalid dataset name: $dsName")
                sys.exit(1)
              }
            }
            println(Format.datasetToString(dataset))
          } catch {
            case e: ConnectionError => {
              Console.err.println(e.getMessage)
              sys.exit(1)
            }
          }
        }

        case List(args.datasetsCmd, args.datasetsCmd.previewTsvCmd) => {
          val fileName = args.datasetsCmd.uploadCmd.file()
          val name = args.datasetsCmd.uploadCmd.name.get.getOrElse(fileName)
          val src = Source.fromFile(args.datasetsCmd.uploadCmd.file())
          try {
            val tsvData = TsvDataset.fromSource(name, src)
            val data = tsvData.data.toSeq
            var dataset = tsvData.asDataset.copy(size = Some(data.size))
            dataset.dtype match {
              case Dataset.ClassificationType => {
                val classCounts = {
                  val classes = for {
                    datum <- data
                    Right(cls) <- datum.value
                  } yield cls
                  classes.groupBy(identity).mapValues(_.size)
                }
                dataset = dataset.copy(classCounts = Some(classCounts))
              }
              case Dataset.RegressionType => {
                val scores = for {
                  datum <- data
                  Left(score) <- datum.value
                } yield score
                val vec = DenseVector(scores: _*)
                val dmin = min(vec)
                val dmax = max(vec)
                val dmean = mean(vec)
                val dstddev = stddev(vec)
                val dmedian = median(vec)
                val p10 = percentile(scores, 0.1)
                val p90 = percentile(scores, 0.9)
                val stats = Dataset.RegressionSummaryStats(dmin, dmax, dmean, dstddev, dmedian, p10, p90)
                dataset = dataset.copy(summaryStats = Some(stats))
              }
            }
            println(Format.datasetToString(dataset))
          } finally {
            src.close()
          }
        }

        case _ => println("You did not supply an argument -- try 'levar-cli datasets'")
      }
    } catch {
      case e: Throwable => {
        Console.err.println(s"Error: ${e.getMessage}")
        sys.exit(1)
      }
    } finally {
      sys.exit
    }
  }
}
