package levar

import org.rogach.scallop._
import org.rogach.scallop.exceptions._
import org.joda.time.format.DateTimeFormat
import com.typesafe.config.ConfigFactory
import levar.client._
import levar.io._
import levar.util._
import scala.io.Source
import scala.concurrent.Await
import scala.concurrent.duration._
import java.io.{ File, FileOutputStream, PrintStream, BufferedOutputStream }
import breeze.linalg._
import breeze.stats._
import breeze.stats.DescriptiveStats._

/** CLI runner class */
object LevarCli {

  implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext

  val aPatt = """([-\w\.]+)""".r
  val abPatt = """([-\w\.]+)/([-\w\.]+)""".r
  val abcPatt = """([-\w\.]+)/([-\w\.]+)/([-\w\.]+)""".r

  def readLine(prompt: String): String = {
    print(prompt)
    Console.in.readLine.trim
  }

  def main(argv: Array[String]) {

    try {

      // Settings -- app defaults and environment variables
      val conf = ConfigFactory.load

      val waitTime = conf.getInt("levar.standard_api_wait")

      /// CLI args
      val args = new ScallopConf(argv) {

        version(s"LeVar CLI (client v${levar.build.BuildInfo.version})")

        banner("""
         |Command-line interface to a LeVar evaluation dataset service
         |
         |Usage: levar-cli [Options] [Subcommmand] [Subcommand params]
         |
         |Options:""".stripMargin)

        val helpCmd = new Subcommand("help") {
          footer("")
          val cmdTopic = trailArg[String](required = false, descr = "Command user is seeking help on")
        }

        val configCmd = new Subcommand("config") {
          banner(" Configure your client\n\n options:")
          footer("")

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

        val listCmd = new Subcommand("list") {
          footer("")

          val datasetsCmd = new Subcommand("datasets") {
            banner(" List datasets in your organization")
            footer("")

            val namesOnly = toggle(name = "namesonly", default = Some(false), descrYes = "Only display dataset names")
            val org = trailArg[String](required = false, descr = "Your organization")
          }

          val experimentsCmd = new Subcommand("experiments") {
            banner(" List experiments associated with a dataset")
            footer("")

            val dataset = trailArg[String]("dataset", required = false, descr = "List experiments for this dataset")
          }
        }

        val uploadCmd = new Subcommand("upload") {
          footer("")

          val datasetCmd = new Subcommand("dataset") {
            banner(" Create a new dataset and upload a TSV file\n\n options:")
            footer("")
            val name = opt[String]("name", descr = "Name for the dataset (default = the filename)")
            val org = opt[String]("org", descr = "Upload dataset to a specific organization")
            val file = trailArg[String](required = true, descr = "TSV file for upload")
          }

          val experimentCmd = new Subcommand("experiment") {
            banner(" Upload a TSV experiment file to database\n\n options:")
            footer("")
            val name = opt[String]("name", descr = "Name for the dataset (default = the filename)")
            val dataset = trailArg[String](required = true, descr = "Dataset associated with the experiment")
            val file = trailArg[String](required = true, descr = "TSV file for upload; must include id and score/class columns")
          }
        }

        val viewCmd = new Subcommand("view") {
          footer("")

          val datasetCmd = new Subcommand("dataset") {
            banner(" View a dataset")
            footer("")
            val dataset = trailArg[String](required = true, descr = "Dataset to view (by ID or org/id pattern)")
          }

          val experimentCmd = new Subcommand("experiment") {
            banner(" View a summary of experiment results")
            footer("")
            val experiment = trailArg[String](required = true, descr = "Experiment to view -- specify like DATASET/EXPERIMENT")
          }
        }

        val renameCmd = new Subcommand("rename") {
          footer("")

          val datasetCmd = new Subcommand("dataset") {
            banner(" Rename a dataset")
            footer("")

            val from = trailArg[String](required = true, descr = "Dataset to rename")
            val to = trailArg[String](required = true, descr = "New name for the dataset")
          }

          val experimentCmd = new Subcommand("experiment") {
            banner(" Rename a experiment")
            footer("")

            val from = trailArg[String](required = true, descr = "Experiment to rename -- specify like DATASET/EXPERIMENT")
            val to = trailArg[String](required = true, descr = "New name for the experiment (omit dataset path)")
          }
        }

        val deleteCmd = new Subcommand("delete") {
          footer("")

          val datasetCmd = new Subcommand("dataset") {
            banner(" Delete a dataset from the database")
            footer("")
            val dataset = trailArg[String](required = true, descr = "Dataset to delete")
          }

          val experimentCmd = new Subcommand("experiment") {
            banner(" Delete an experiment")
            footer("")
            val experiment = trailArg[String](required = true, descr = "Experiment to delete -- specify like DATASET/EXPERIMENT")
          }
        }

        val downloadCmd = new Subcommand("download") {
          footer("")

          val datasetCmd = new Subcommand("dataset") {
            banner(" Download a dataset from the database. By default does *not* include gold labels/values\n\n options:")
            footer("")

            val output = opt[String]("output", descr = "File to save dataset to")
            val gold = toggle(name = "gold", default = Some(false), descrYes = "Include gold labels/values in output")
            val dataset = trailArg[String](required = true, descr = "Dataset to view")
          }
        }

        val previewCmd = new Subcommand("preview") {
          val datasetCmd = new Subcommand("dataset") {
            banner(" Ingest a TSV dataset and see a high level summary.\n\n options:")

            footer("")
            val name = opt[String]("name", descr = "Name for the dataset (default = the filename)")
            val file = trailArg[String](required = true, descr = "TSV file for upload")
          }
        }
      }

      args.subcommands match {

        case List(args.helpCmd) => {

          var generalHelp = 
"""    ~optional param~ <required param>
config
list datasets ~organization name~  
list expiriements <dataset name>   # Dataset name name is optional in LevarCli.scala, but appears to be required
view dataset <dataset name>
view experiment <experiment name>"""
          var generalHelp2 = 
"""config
list [datasets, expiriements]"""
          var generalHelp3 = 
"""config
list"""


          val cmdTopic = args.helpCmd.cmdTopic.get.getOrElse("") 
          val commandHelp = cmdTopic match {
            case ""       => ""
            case "config" => "config command description"
            case "list"   => "list command description"
            case _        => s"Invalid command name: $cmdTopic"
          }

          if (cmdTopic == "") println(generalHelp)
          else                println(commandHelp)
        }

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

        case List(args.listCmd, args.listCmd.datasetsCmd) => {
          val client = ClientConfigIo.loadClient
          val org = args.listCmd.datasetsCmd.org.get.getOrElse(client.config.org)
          try {
            val datasetRS = Await.result(client.searchDatasets(org), waitTime seconds)
            if (datasetRS.nonEmpty) {
              println(s"Datasets for $org")
              println
              if (args.listCmd.datasetsCmd.namesOnly())
                println(Format.datatsetRSnames(datasetRS))
              else
                println(Format.datasetRStoString(datasetRS))

              println
            } else {
              println(s"No datasets for $org")
            }
          } catch {
            case e: ConnectionError if e.getMessage.startsWith("404") => {
              Console.err.println(s"Could not access datasets for $org")
              sys.exit(1)
            }
            case e: ConnectionError => {
              Console.err.println(e.getMessage)
              sys.exit(1)
            }
          }
        }

        case List(args.uploadCmd, args.uploadCmd.datasetCmd) => {
          try {
            val client = ClientConfigIo.loadClient
            val fileName = args.uploadCmd.datasetCmd.file()
            val org = args.uploadCmd.datasetCmd.org.get
            if (!(fileName.endsWith(".tsv") || fileName.endsWith(".txt"))) {
              throw new RuntimeException("Expected to see a TSV file (.tsv or .txt)")
            }
            val name = args.uploadCmd.datasetCmd.name.get.getOrElse(fileName)
            val src = Source.fromFile(args.uploadCmd.datasetCmd.file())
            try {
              val tabular = TsvDataset.fromSource(name, src)
              val dataset = tabular.asDataset
              Await.result(client.createDataset(dataset, org), waitTime seconds)
              for (data <- tabular.data.grouped(250)) {
                val d = data.toSeq
                Await.result(client.uploadDatasetData(dataset.id, d, org), waitTime seconds)
              }
              val saved = Await.result(client.getDataset(org, name), waitTime seconds)
              println(Format.datasetToString(saved))
              println
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

        case List(args.viewCmd, args.viewCmd.datasetCmd) => {
          val dsName = args.viewCmd.datasetCmd.dataset()
          val client = ClientConfigIo.loadClient
          val (org, datasetId) = dsName match {
            case abPatt(org, datasetId) => (org, datasetId)
            case aPatt(datasetId) => (client.config.org, datasetId)
            case _ => {
              Console.err.println(s"Invalid dataset name: $dsName")
              sys.exit(1)
            }
          }
          try {
            val dataset = Await.result(client.getDataset(Some(org), datasetId), waitTime seconds)
            println(Format.datasetToString(dataset))
            println
          } catch {
            case e: ConnectionError if e.getMessage.startsWith("404") => {
              Console.err.println(s"Dataset not found: $org/$datasetId")
              sys.exit(1)
            }
            case e: ConnectionError => {
              Console.err.println(e.getMessage)
              sys.exit(1)
            }
          }
        }

        case List(args.previewCmd, args.previewCmd.datasetCmd) => {
          val fileName = args.previewCmd.datasetCmd.file()
          val name = args.previewCmd.datasetCmd.name.get.getOrElse(fileName)
          val src = Source.fromFile(args.previewCmd.datasetCmd.file())
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
            println
          } finally {
            src.close()
          }
        }

        case List(args.deleteCmd, args.deleteCmd.datasetCmd) => {
          val dsName = args.deleteCmd.datasetCmd.dataset()
          val client = ClientConfigIo.loadClient
          val (org, datasetId) = dsName match {
            case abPatt(org, datasetId) => (org, datasetId)
            case aPatt(datasetId) => (client.config.org, datasetId)
            case _ => {
              Console.err.println(s"Invalid dataset name: $dsName")
              sys.exit(1)
            }
          }
          println(
            """|Are you sure you want to delete this dataset and associated experiments?
               |To confirm, type your organization and dataset name (like "org/dataset")""".stripMargin)

          readLine("> ").trim match {
            case abPatt(orgVerify, datasetIdVerify) if orgVerify == org && datasetIdVerify == datasetId => {
              Await.result(client.deleteDataset(org, datasetId), waitTime seconds)
              println("Deleted")
            }
            case _ => {
              Console.err.println("Org + dataset do not match")
              sys.exit(1)
            }
          }
        }

        case List(args.downloadCmd, args.downloadCmd.datasetCmd) => {
          val useGold = args.downloadCmd.datasetCmd.gold.get == Some(true)
          val dsName = args.downloadCmd.datasetCmd.dataset()
          val client = ClientConfigIo.loadClient
          try {
            val (org, datasetId) = dsName match {
              case abPatt(org, datasetId) => (org, datasetId)
              case aPatt(datasetId) => (client.config.org, datasetId)
              case _ => {
                Console.err.println(s"Invalid dataset name: $dsName")
                sys.exit(1)
              }
            }

            val dataset = Await.result(client.getDataset(org, datasetId), waitTime seconds)

            val cols = Seq("id") ++ dataset.columns ++ {
              if (useGold) Seq(dataset.dtype.colname) else Seq.empty
            }

            val stream: PrintStream = args.downloadCmd.datasetCmd.output.get match {
              case Some(outputFilename) => {
                new PrintStream(
                  new BufferedOutputStream(
                    new FileOutputStream(
                      new File(outputFilename))))
              }
              case None => {
                Console.out
              }
            }

            try {
              stream.println(cols.mkString("\t"))
              var rs = Await.result(client.fetchData(org, datasetId, useGold), waitTime seconds)
              while (rs.nonEmpty) {
                for (datum <- rs) {
                  stream.println(datum.dataByCol(cols).map(_.toString).mkString("\t"))
                }
                rs.lastOption.flatMap(_.id) foreach { id =>
                  rs = Await.result(client.fetchData(org, datasetId, useGold, id), waitTime seconds)
                }
              }
            } finally {
              stream.close()
            }

          } catch {
            case e: ConnectionError => {
              Console.err.println(e.getMessage)
              sys.exit(1)
            }
          }
        }

        case List(args.listCmd, args.listCmd.experimentsCmd) => {
          val dsNameOpt = args.listCmd.experimentsCmd.dataset.get
          val client = ClientConfigIo.loadClient
          try {
            val experimentRS = dsNameOpt match {
              case Some(dsName) => {
                val (datasetOrg, datasetId) = dsName match {
                  case abPatt(datasetOrg, datasetId) => (datasetOrg, datasetId)
                  case aPatt(datasetId) => (client.config.org, datasetId)
                  case _ => {
                    Console.err.println(s"Invalid dataset name: $dsName")
                    sys.exit(1)
                  }
                }
                println(s"Experiments for dataset $datasetOrg/$datasetId")
                Await.result(client.searchExperiments(datasetOrg, datasetId), waitTime seconds)
              }
              case None => {
                Console.err.println("Please provide a dataset ID")
                sys.exit(1)
              }
            }
            println
            println(Format.experimentRStoString(experimentRS))
            println

          } catch {
            case e: ConnectionError => {
              Console.err.println(e.getMessage)
              sys.exit(1)
            }
          }
        }

        case List(args.uploadCmd, args.uploadCmd.experimentCmd) => {
          val client = ClientConfigIo.loadClient
          val fileName = args.uploadCmd.experimentCmd.file()
          val dsName = args.uploadCmd.experimentCmd.dataset()
          val (org, datasetId) = dsName match {
            case abPatt(datasetOrg, datasetId) => (datasetOrg, datasetId)
            case aPatt(datasetId) => (client.config.org, datasetId)
            case _ => {
              Console.err.println(s"Invalid dataset name: $dsName")
              sys.exit(1)
            }
          }

          if (!(fileName.endsWith(".tsv") || fileName.endsWith(".txt"))) {
            throw new RuntimeException("Expected to see a TSV file (.tsv or .txt)")
          }

          val name = args.uploadCmd.experimentCmd.name.get.getOrElse(fileName)

          val src = Source.fromFile(args.uploadCmd.experimentCmd.file())

          try {
            val dataset = Await.result(client.getDataset(Some(org), datasetId), waitTime seconds)
            val tabular = TsvExperiment.fromSource(dataset.dtype, name, src)
            val experiment = tabular.asExperiment
            Await.result(client.createExperiment(org, datasetId, experiment), waitTime seconds)
            for (data <- tabular.data.grouped(1000)) {
              val d = data.toSeq
              Await.result(client.uploadExperimentData(org, datasetId, experiment.id, d), waitTime seconds)
            }
            val saved = Await.result(client.getExperiment(org, datasetId, name), waitTime seconds)
            println(Format.experimentToString(saved))
            println
          } catch {
            case e: ConnectionError => {
              Console.err.println("Could not upload datset")
              Console.err.println(e.getMessage)
              sys.exit(1)
            }
          } finally {
            src.close()
          }
        }

        case List(args.renameCmd, args.renameCmd.datasetCmd) => {
          val dsName = args.renameCmd.datasetCmd.from()
          val client = ClientConfigIo.loadClient
          val (org, datasetId) = dsName match {
            case abPatt(org, datasetId) => (org, datasetId)
            case aPatt(datasetId) => (client.config.org, datasetId)
            case _ => {
              Console.err.println(s"Invalid dataset name: $dsName")
              sys.exit(1)
            }
          }
          val newName = args.renameCmd.datasetCmd.to()
          val newDatasetId = newName match {
            case aPatt(newDsId) => newDsId
            case _ => {
              Console.err.println(s"Invalid dataset name: $newName")
              sys.exit(1)
            }
          }
          Await.result(client.renameDataset(org, datasetId, newDatasetId), waitTime seconds)
          println(s"Dataset $org/$datasetId renamed to $org/$newDatasetId")
        }

        case List(args.deleteCmd, args.deleteCmd.experimentCmd) => {
          val client = ClientConfigIo.loadClient
          val expName = args.deleteCmd.experimentCmd.experiment()
          val (org, datasetId, experimentId) = expName match {
            case abcPatt(orgId, datasetId, experimentId) => (orgId, datasetId, experimentId)
            case abPatt(datasetId, experimentId) => (client.config.org, datasetId, experimentId)
            case _ => {
              Console.err.println(s"Invalid experiment name: $expName")
              Console.err.println("Please specify as DATASET/EXPERIMENT or ORG/DATASET/EXPERIMENT")
              sys.exit(1)
            }
          }
          println(
            """|Are you sure you want to delete this experiment?
               |To confirm, type the experiment name""".stripMargin)

          if (readLine("> ").trim == experimentId) {
            Await.result(client.deleteExperiment(org, datasetId, experimentId), waitTime seconds)
            println("Deleted")
          } else {
            Console.err.println("Org + dataset do not match")
            sys.exit(1)
          }
        }

        case List(args.viewCmd, args.viewCmd.experimentCmd) => {
          val client = ClientConfigIo.loadClient
          val expName = args.deleteCmd.experimentCmd.experiment()
          val (org, datasetId, experimentId) = expName match {
            case abcPatt(orgId, datasetId, experimentId) => (orgId, datasetId, experimentId)
            case abPatt(datasetId, experimentId) => (client.config.org, datasetId, experimentId)
            case _ => {
              Console.err.println(s"Invalid experiment name: $expName")
              Console.err.println("Please specify as DATASET/EXPERIMENT or ORG/DATASET/EXPERIMENT")
              sys.exit(1)
            }
          }
          val experiment = Await.result(client.getExperiment(org, datasetId, experimentId), waitTime seconds)
          println(Format.experimentToString(experiment))
          println
        }

        case _ => println("You did not supply an argument -- try 'levar-cli list datasets'")
      }
    } catch {

      case _: MissingClientConfig => {
        Console.err.println("Missing cofig -- run `levar config` to set up")
        sys.exit(1)
      }

      case e: Throwable => {
        Console.err.println(s"Error: ${e.getMessage}")
        sys.exit(1)
      }

    } finally {
      sys.exit
    }
  }
}
