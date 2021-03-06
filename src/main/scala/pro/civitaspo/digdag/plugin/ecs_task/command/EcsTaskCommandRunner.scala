package pro.civitaspo.digdag.plugin.ecs_task.command
import com.google.common.base.Optional
import io.digdag.client.config.{Config, ConfigFactory}
import io.digdag.spi.TaskResult
import io.digdag.util.DurationParam
import org.slf4j.Logger
import pro.civitaspo.digdag.plugin.ecs_task.VERSION
import pro.civitaspo.digdag.plugin.ecs_task.aws.AwsConf

import scala.collection.JavaConverters._

case class EcsTaskCommandRunner(
  tmpStorage: TmpStorage,
  mainScript: String,
  params: Config,
  environments: Map[String, String],
  awsConf: AwsConf,
  logger: Logger
) {

  val cf: ConfigFactory = params.getFactory

  // For ecs_task.register>  operator (TaskDefinition)
  // NOTE: Use only 1 container
  // val containerDefinitions: Seq[ContainerDefinition] = params.parseList("container_definitions", classOf[Config]).asScala.map(configureContainerDefinition).map(_.get)
  val sidecars: Seq[Config] = params.parseListOrGetEmpty("sidecars", classOf[Config]).asScala
  val cpu: Optional[String] = params.getOptional("cpu", classOf[String])
  val executionRoleArn: Optional[String] = params.getOptional("execution_role_arn", classOf[String])

  val taskName: String = params.get("task_name", classOf[String])
  val familyPrefix: String = params.get("family_prefix", classOf[String], "")
  val familySuffix: String = params.get("family_suffix", classOf[String], "")
  val familyInfix: String = params.get("family_infix", classOf[String], taskName.replaceAll("\\+", "_").replaceAll("\\^", "_").replaceAll("\\=", "_"))
  val family: String = params.get("family", classOf[String], s"$familyPrefix$familyInfix$familySuffix")
  val memory: Optional[String] = params.getOptional("memory", classOf[String])
  val networkMode: Optional[String] = params.getOptional("network_mode", classOf[String])
  // NOTE: Use `ecs_task.run>`'s one.
  // val placementConstraints: Seq[TaskDefinitionPlacementConstraint] = params.parseListOrGetEmpty("placement_constraints", classOf[Config]).asScala.map(configureTaskDefinitionPlacementConstraint).map(_.get)
  val requiresCompatibilities: Seq[String] = params.parseListOrGetEmpty("requires_compatibilities", classOf[String]).asScala // Valid Values: EC2 | FARGATE
  val taskRoleArn: Optional[String] = params.getOptional("task_role_arn", classOf[String])
  val volumes: Seq[Config] = params.parseListOrGetEmpty("volumes", classOf[Config]).asScala

  // For `ecs_task.register>` operator (ContainerDefinition)
  // NOTE: Set by this plugin
  // val command: Seq[String] = params.parseListOrGetEmpty("command", classOf[String]).asScala
  // NOTE: Set in `ecs_task.register>` TaskDefinition Context. If you set it by container level, use the `overrides` option.
  // val cpu: Optional[Int] = params.getOptional("cpu", classOf[Int])
  val disableNetworking: Optional[Boolean] = params.getOptional("disable_networking", classOf[Boolean])
  val dnsSearchDomains: Seq[String] = params.parseListOrGetEmpty("dns_search_domains", classOf[String]).asScala
  val dnsServers: Seq[String] = params.parseListOrGetEmpty("dns_servers", classOf[String]).asScala
  // NOTE: Add some labels by this plugin
  val dockerLabels: Map[String, String] = params.getMapOrEmpty("docker_labels", classOf[String], classOf[String]).asScala.toMap
  val dockerSecurityOptions: Seq[String] = params.parseListOrGetEmpty("docker_security_options", classOf[String]).asScala
  val entryPoint: Seq[String] = params.parseListOrGetEmpty("entry_point", classOf[String]).asScala
  // NOTE: Add some envs by this plugin
  val configEnvironment: Map[String, String] = params.getMapOrEmpty("environments", classOf[String], classOf[String]).asScala.toMap
  // NOTE: This plugin uses only 1 container so `essential` is always true.
  // val essential: Optional[Boolean] = params.getOptional("essential", classOf[Boolean])
  val extraHosts: Map[String, String] = params.getMapOrEmpty("extra_hosts", classOf[String], classOf[String]).asScala.toMap
  val healthCheck: Optional[Config] = params.getOptionalNested("health_check")
  val hostname: Optional[String] = params.getOptional("hostname", classOf[String])
  val image: Optional[String] = params.getOptional("image", classOf[String])
  val interactive: Optional[Boolean] = params.getOptional("interactive", classOf[Boolean])
  val links: Seq[String] = params.parseListOrGetEmpty("links", classOf[String]).asScala
  val linuxParameters: Optional[Config] = params.getOptionalNested("linux_parameters")
  val logConfiguration: Optional[Config] = params.getOptionalNested("log_configuration")
  // NOTE: Set in `ecs_task.register>` TaskDefinition Context. If you set it by container level, use the `overrides` option.
  // val memory: Optional[Int] = params.getOptional("memory", classOf[Int])
  // NOTE: If you set it by container level, use the `overrides` option.
  // val memoryReservation: Optional[Int] = params.getOptional("memory_reservation", classOf[Int])
  val mountPoints: Seq[Config] = params.parseListOrGetEmpty("mount_points", classOf[Config]).asScala
  val containerName: String = params.get("container_name", classOf[String], family)
  val portMappings: Seq[Config] = params.parseListOrGetEmpty("port_mappings", classOf[Config]).asScala
  val privileged: Optional[Boolean] = params.getOptional("privileged", classOf[Boolean])
  val pseudoTerminal: Optional[Boolean] = params.getOptional("pseudo_terminal", classOf[Boolean])
  val readonlyRootFilesystem: Optional[Boolean] = params.getOptional("readonly_root_filesystem", classOf[Boolean])
  val repositoryCredentials: Optional[Config] = params.getOptionalNested("repository_credentials")
  val systemControls: Seq[Config] = params.parseListOrGetEmpty("system_controls", classOf[Config]).asScala
  val ulimits: Seq[Config] = params.parseListOrGetEmpty("ulimits", classOf[Config]).asScala
  val user: Optional[String] = params.getOptional("user", classOf[String])
  val volumesFrom: Seq[Config] = params.parseListOrGetEmpty("volumes_from", classOf[Config]).asScala
  val workingDirectory: Optional[String] = params.getOptional("working_directory", classOf[String])

  // For ecs_task.run operator
  val cluster: String = params.get("cluster", classOf[String])
  val count: Optional[Int] = params.getOptional("count", classOf[Int])
  val group: Optional[String] = params.getOptional("group", classOf[String])
  val launchType: Optional[String] = params.getOptional("launch_type", classOf[String])
  val networkConfiguration: Optional[Config] = params.getOptionalNested("network_configuration")
  val overrides: Optional[Config] = params.getOptionalNested("overrides")
  val placementConstraints: Seq[Config] = params.parseListOrGetEmpty("placement_constraints", classOf[Config]).asScala
  val placementStrategy: Seq[Config] = params.parseListOrGetEmpty("placement_strategy", classOf[Config]).asScala
  val platformVersion: Optional[String] = params.getOptional("platform_version", classOf[String])
  val startedBy: Optional[String] = params.getOptional("started_by", classOf[String])
  // NOTE: Generated by ecs_task.register operator
  // val taskDefinition: String = params.get("task_definition", classOf[String])

  // For ecs_task.wait operator
  val timeout: DurationParam = params.get("timeout", classOf[DurationParam], DurationParam.parse("15m"))

  def run(): TaskResult = {
    val subTasks: Config = cf.create()
    subTasks.setNested("+register", ecsTaskRegisterSubTask())
    subTasks.setNested("+run", ecsTaskRunInternalSubTask())
    subTasks.setNested("+wait", ecsTaskWaitSubTask())
    subTasks.setNested("+result", ecsTaskResultSubTask())

    val builder = TaskResult.defaultBuilder(cf)
    builder.subtaskConfig(subTasks)
    builder.build()
  }

  protected def ecsTaskRegisterSubTask(): Config = {
    withDefaultSubTask { subTask =>
      subTask.set("_type", "ecs_task.register")
      subTask.set("_command", taskDefinitionConfig())
    }
  }

  protected def ecsTaskRunInternalSubTask(): Config = {
    withDefaultSubTask { subTask =>
      subTask.set("_type", "ecs_task.run_internal")
      subTask.set("cluster", cluster)
      subTask.setOptional("count", count)
      subTask.setOptional("group", group)
      subTask.setOptional("launch_type", launchType)
      subTask.setOptional("network_configuration", networkConfiguration)
      subTask.setOptional("overrides", overrides)
      subTask.set("placement_constraints", placementConstraints.asJava)
      subTask.set("placement_strategy", placementStrategy.asJava)
      subTask.setOptional("platform_version", platformVersion)
      subTask.setOptional("started_by", startedBy)
      subTask.set("task_definition", "${last_ecs_task_register.task_definition_arn}")
    }
  }

  protected def ecsTaskWaitSubTask(): Config = {
    withDefaultSubTask { subTask =>
      subTask.set("_type", "ecs_task.wait")
      subTask.set("cluster", cluster)
      subTask.set("tasks", "${last_ecs_task_run.task_arns}")
      subTask.set("timeout", timeout.toString)
      subTask.set("ignore_failure", true)
    }
  }

  protected def ecsTaskResultSubTask(): Config = {
    withDefaultSubTask { subTask =>
      subTask.set("_type", "ecs_task.command_result_internal")
      subTask.set("_command", tmpStorage.getLocation)
    }
  }

  protected def withDefaultSubTask(f: Config => Config): Config = {
    val subTask: Config = cf.create()

    subTask.set("auth_method", awsConf.authMethod)
    subTask.set("profile_name", awsConf.profileName)
    if (awsConf.profileFile.isPresent) subTask.set("profile_file", awsConf.profileFile.get())
    subTask.set("use_http_proxy", awsConf.useHttpProxy)
    if (awsConf.region.isPresent) subTask.set("region", awsConf.region.get())
    if (awsConf.endpoint.isPresent) subTask.set("endpoint", awsConf.endpoint.get())

    f(subTask)
    subTask
  }

  protected def taskDefinitionConfig(): Config = {
    val c: Config = cf.create()

    c.set("container_definitions", (Seq(containerDefinitionConfig()) ++ sidecars).asJava)
    c.setOptional("cpu", cpu)
    c.setOptional("execution_role_arn", executionRoleArn)
    c.set("family", family)
    c.setOptional("memory", memory)
    c.setOptional("network_mode", networkMode)
    c.set("requires_compatibilities", requiresCompatibilities.asJava)
    c.setOptional("task_role_arn", taskRoleArn)
    c.set("volumes", volumes.asJava)

    c
  }

  protected def containerDefinitionConfig(): Config = {
    val c: Config = cf.create()

    val command: Seq[String] = tmpStorage.buildTaskCommand(mainScript)
    logger.info(s"Run in the container: ${command.mkString(" ")}")
    c.set("command", command.asJava)
    c.setOptional("disable_networking", disableNetworking)
    c.set("dns_search_domains", dnsSearchDomains.asJava)
    c.set("dns_servers", dnsServers.asJava)
    val additionalLabels: Map[String, String] = Map("pro.civitaspo.digdag.plugin.ecs_task.version" -> VERSION)
    c.set("docker_labels", (dockerLabels ++ additionalLabels).asJava)
    c.set("entry_point", entryPoint.asJava)
    c.set("environment", (configEnvironment ++ environments).asJava)
    c.set("essential", true)
    c.set("extra_hosts", extraHosts.asJava)
    c.setOptional("health_check", healthCheck)
    c.setOptional("image", image)
    c.setOptional("interactive", interactive)
    c.set("links", links.asJava)
    c.setOptional("linux_parameters", linuxParameters)
    c.setOptional("log_configuration", logConfiguration)
    c.set("mount_points", mountPoints.asJava)
    c.set("name", containerName)
    c.set("port_mappings", portMappings.asJava)
    c.setOptional("privileged", privileged)
    c.setOptional("pseudo_terminal", pseudoTerminal)
    c.setOptional("readonly_root_filesystem", readonlyRootFilesystem)
    c.setOptional("repository_credentials", repositoryCredentials)
    c.set("system_controls", systemControls.asJava)
    c.set("ulimits", ulimits.asJava)
    c.setOptional("user", user)
    c.set("volumes_from", volumesFrom.asJava)
    c.setOptional("working_directory", workingDirectory)

    c
  }

}
