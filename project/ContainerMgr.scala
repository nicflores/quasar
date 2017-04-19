
import java.io.File
import scala.sys.process._
import scala.collection.JavaConverters._
import scala.collection.immutable.List
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.model.{Container, ExposedPort, Ports}
import com.github.dockerjava.core.{DefaultDockerClientConfig, DockerClientBuilder}
import com.github.dockerjava.api.command._
import com.github.dockerjava.core.command.BuildImageResultCallback
import java.lang.String
import java.lang.Integer
import scala.Int

object ContainerMgr {
  // TODO: configure this without hard coding these values
  val dockerConfig = DefaultDockerClientConfig.createDefaultConfigBuilder
    .withDockerHost("tcp://192.168.99.101:2376")
    .withDockerTlsVerify(java.lang.Boolean.TRUE)
    .withDockerCertPath("/Users/nicandroflores/.docker/machine/machines/default").build

  val dockerClient: DockerClient = DockerClientBuilder.getInstance(dockerConfig).build

  /** A function to build lists of ports a container should expose
    *
    * @param portList A list of ports the container should expose to the host
    * @return         A list of com.github.dockerjava.api.model.ExposedPort
    */
  def exposedPorts(portList: List[Int]): List[ExposedPort] = {
    portList.map(ExposedPort.tcp(_))
  }

  /** A function to create a list of port bindings
    *
    * @param portList A list of ports to bind
    * @return         A list of port binding
    */
  def portBindings(portList: List[Int]): List[Ports.Binding] = {
    portList.map(Ports.Binding.bindPort(_))
  }

  /** a function to bind ports between the host and the container
    *
    * @param eports A list of ports the container should expose to the host
    * @param bports A list of ports on the host to bind to the exposed ports
    * @return       com.github.dockerjava.api.model.Ports
    */
  def bindPorts(eports: List[ExposedPort], bports: List[Ports.Binding]): Ports = {
    val portBindings = new Ports()
    eports.zip(bports).foreach(t => portBindings.bind(t._1,t._2))
    portBindings
  }

  /** get quasar related list of containers
    *
    * @return
    */
  def getContainers: List[Container] = {
    dockerClient.listContainersCmd.withShowAll(java.lang.Boolean.TRUE).exec().asScala.toList.filter(c => c.getNames.apply(0).contains("quasar_"))
  }

  /** start a container from a container command
    *
    * @param ccc
    * @return
    */
  def startContainer(ccc: CreateContainerCmd): StartContainerCmd = {
    dockerClient.startContainerCmd(ccc.exec.getId)
  }

  /** stop a containers
    *
    * @param container
    * @return
    */
  def stopContainer(container: Container): StopContainerCmd = {
    dockerClient.stopContainerCmd(container.getId()).withTimeout(java.lang.Integer.valueOf(2))
  }

  /** delete a containers
    *
    * @param container
    * @return
    */
  def deleteContainer(container: Container): RemoveContainerCmd = {
    dockerClient.removeContainerCmd(container.getId())
  }

  /** build docker image from a build image command
    *
    * @param bic The result of calling dockerClient.createContainerCmd
    * @return
    */
  def buildImage(bic: BuildImageCmd): String = {
    bic.exec(new BuildImageResultCallback()).awaitImageId
  }

  /** create a mongo container command
    *
    * @param bports
    * @param containerName
    * @param image
    * @return
    */
  def createMongoQuasarContainer(bports: List[Int], containerName: String, image: String): CreateContainerCmd = {
    val eports = List(27017)
    dockerClient.createContainerCmd(image)
      .withCmd("mongod", "--smallfiles")
      .withExposedPorts(exposedPorts(eports).asJava)
      .withPortBindings(bindPorts(exposedPorts(eports),portBindings(bports)))
      .withName(containerName)
  }

  /** create a portgresql container command
    *
    * @param bports
    * @param containerName
    * @param image
    * @return
    */
  def createPostgresqlQuasarContainer(bports: List[Int], containerName: String, image: String): CreateContainerCmd = {
    val eports = List(5432)
    dockerClient.createContainerCmd(image)
      .withExposedPorts(exposedPorts(eports).asJava)
      .withPortBindings(bindPorts(exposedPorts(eports),portBindings(bports)))
      .withName(containerName)
  }

  /** create a couchbase container command
    *
    * @param bports
    * @param containerName
    * @param image
    * @return
    */
  def createCouchbaseQuasarContainer(bports: List[Int], containerName: String, image: String): CreateContainerCmd = {
    val eports = List(8091,8092,8093,8094,11210)
    dockerClient.createContainerCmd(image)
      .withExposedPorts(exposedPorts(eports).asJava)
      .withPortBindings(bindPorts(exposedPorts(eports),portBindings(bports)))
      .withName(containerName)
  }

  /** create marklogic image command, needs to be exec'ed by buildImage()
    *
    * @param name The name of the image will be given when built
    * @return
    */
  def imageInstructions(name: String, dockerfile: String): BuildImageCmd = {
    val df = new File(dockerfile)
    val dfp = new File(df.getParent)
    dockerClient.buildImageCmd()
      .withBaseDirectory(dfp)
      .withDockerfile(df)
      .withTag(name)
      .withNoCache(java.lang.Boolean.FALSE)
  }

  /** create a marklogic container command
    *
    * @param bports
    * @param name
    * @return
    */
  def createMarklogicQuasarContainer(bports: List[Int], name: String, image: String): CreateContainerCmd = {
    val eports = List(8000,8001,8002)
    dockerClient.createContainerCmd(buildImage(imageInstructions("marklogic", image)))
      .withExposedPorts(exposedPorts(eports).asJava)
      .withPortBindings(bindPorts(exposedPorts(eports),portBindings(bports)))
      .withName(name)
  }

  /**
    *
    * @param container
    * @return
    */
  // TODO: figure out dockerfile path without hardcoding them
  def start(container: String): StartContainerCmd = {
    val dockerfile = "/Users/nicandroflores/Documents/github/quasar/docker/Dockerfiles/Marklogic/MarkLogic-Dockerfile"
    container match {
      case "quasar_mongodb_2_6" => startContainer(createMongoQuasarContainer(List(27018), "quasar_mongodb_2_6", "tutum/mongodb:2.6"))
      case "quasar_mongodb_3_0" => startContainer(createMongoQuasarContainer(List(27019), "quasar_mongodb_3_0", "mongo:3.0"))
      case "quasar_mongodb_read_only" => startContainer(createMongoQuasarContainer(List(27020), "quasar_mongodb_read_only", "mongo:3.0"))
      case "quasar_mongodb_3_2" => startContainer(createMongoQuasarContainer(List(27021), "quasar_mongodb_3_2", "mongo:3.2"))
      case "quasar_mongodb_3_4" => startContainer(createMongoQuasarContainer(List(27022), "quasar_mongodb_3_4", "mongo:3.4"))
      case "quasar_metastore" => startContainer(createPostgresqlQuasarContainer(List(5432), "quasar_metastore", "postgres:9.6"))
      case "quasar_postgresql" => startContainer(createPostgresqlQuasarContainer(List(5433), "quasar_postgresql", "postgres:9.6"))
      case "quasar_couchbase" => startContainer(createCouchbaseQuasarContainer(List(8091, 8092, 8093, 8094, 11210), "quasar_couchbase", "couchbase/server:enterprise-4.5.1"))
      case "quasar_marklogic_xml" => startContainer(createMarklogicQuasarContainer(List(8000, 8001, 8002), "quasar_marklogic_xml", "/Users/nicandroflores/Documents/github/quasar/docker/Dockerfiles/Marklogic/MarkLogic-Dockerfile"))
      case "quasar_marklogic_json" => startContainer(createMarklogicQuasarContainer(List(9000, 9001, 9002), "quasar_marklogic_json", "/Users/nicandroflores/Documents/github/quasar/docker/Dockerfiles/Marklogic/MarkLogic-Dockerfile"))
      //case _ => "not gonna do anything"
    }
  }

  // TODO: update this to scala instead of shelling out
  def assembleTestingConf = {
    "/Users/nicandroflores/Documents/github/quasar/docker/scripts/assembleTestingConf -a".!!
  }

  // TODO: update this to scala instead of shelling out
  def configureContainers = {
    "/Users/nicandroflores/Documents/github/quasar/docker/scripts/setupContainers -a".!!
  }

  val myQuasarContainers = List("quasar_mongodb_read_only", "quasar_metastore", "quasar_marklogic_xml")

  def startup(containers: List[String]) = {
    containers.map(start(_).exec())
  }

  def setup = {
    configureContainers
    assembleTestingConf
  }

  def stop = {
    getContainers.map(stopContainer(_).exec())
  }

  def cleanup = {
    getContainers.map(deleteContainer(_).exec())
  }
}
