package pro.civitaspo.digdag.plugin.ecs_task.run
import com.amazonaws.services.ecs.model.{
  AwsVpcConfiguration,
  ContainerOverride,
  KeyValuePair,
  NetworkConfiguration,
  PlacementConstraint,
  PlacementConstraintType,
  PlacementStrategy,
  PlacementStrategyType,
  RunTaskRequest,
  RunTaskResult,
  TaskOverride
}
import com.google.common.base.Optional
import com.google.common.collect.ImmutableList
import io.digdag.client.config.{Config, ConfigKey}
import io.digdag.spi.{ImmutableTaskResult, OperatorContext, TaskResult, TemplateEngine}
import pro.civitaspo.digdag.plugin.ecs_task.AbstractEcsTaskOperator

import scala.collection.JavaConverters._

class EcsTaskRunInternalOperator(operatorName: String, context: OperatorContext, systemConfig: Config, templateEngine: TemplateEngine)
    extends AbstractEcsTaskOperator(operatorName, context, systemConfig, templateEngine) {

  val cluster: String = params.get("cluster", classOf[String])
  val count: Optional[Int] = params.getOptional("count", classOf[Int])
  val group: Optional[String] = params.getOptional("group", classOf[String])
  val launchType: Optional[String] = params.getOptional("launch_type", classOf[String])
  val networkConfiguration: Optional[NetworkConfiguration] = configureNetworkConfiguration(params.getNestedOrGetEmpty("network_configuration"))
  val overrides: Optional[TaskOverride] = configureTaskOverride(params.getNestedOrGetEmpty("overrides"))

  val placementConstraints: Seq[PlacementConstraint] =
    params.getListOrEmpty("placement_constraints", classOf[Config]).asScala.map(configurePlacementConstraint).map(_.get)

  val placementStrategy: Seq[PlacementStrategy] =
    params.getListOrEmpty("placement_strategy", classOf[Config]).asScala.map(configurePlacementStrategy).map(_.get)
  val platformVersion: Optional[String] = params.getOptional("platform_version", classOf[String])
  val startedBy: Optional[String] = params.getOptional("started_by", classOf[String])
  val taskDefinition: String = params.get("task_definition", classOf[String]) // generated by ecs_task.register> operator if not set.

  protected def buildRunTaskRequest(): RunTaskRequest = {
    val req: RunTaskRequest = new RunTaskRequest()

    req.setCluster(cluster)
    if (count.isPresent) req.setCount(count.get)
    if (group.isPresent) req.setGroup(group.get)
    if (launchType.isPresent) req.setLaunchType(launchType.get)
    if (networkConfiguration.isPresent) req.setNetworkConfiguration(networkConfiguration.get)
    if (overrides.isPresent) req.setOverrides(overrides.get)
    if (placementConstraints.nonEmpty) req.setPlacementConstraints(placementConstraints.asJava)
    if (placementStrategy.nonEmpty) req.setPlacementStrategy(placementStrategy.asJava)
    if (platformVersion.isPresent) req.setPlatformVersion(platformVersion.get)
    if (startedBy.isPresent) req.setStartedBy(startedBy.get)
    req.setTaskDefinition(taskDefinition)

    req
  }

  protected def configureNetworkConfiguration(c: Config): Optional[NetworkConfiguration] = {
    if (c.isEmpty) return Optional.absent()

    val awsvpcConfiguration: Optional[AwsVpcConfiguration] = configureAwsVpcConfiguration(c.getNestedOrGetEmpty("awsvpc_configuration"))

    val nc: NetworkConfiguration = new NetworkConfiguration()
    if (awsvpcConfiguration.isPresent) nc.setAwsvpcConfiguration(awsvpcConfiguration.get)

    Optional.of(nc)
  }

  protected def configureAwsVpcConfiguration(c: Config): Optional[AwsVpcConfiguration] = {
    if (c.isEmpty) return Optional.absent()

    val assignPublicIp: Optional[String] = c.getOptional("assign_public_ip", classOf[String])
    val securityGroups: Seq[String] = c.getListOrEmpty("security_groups", classOf[String]).asScala
    val subnets: Seq[String] = c.getListOrEmpty("subnets", classOf[String]).asScala

    val avc: AwsVpcConfiguration = new AwsVpcConfiguration()
    if (assignPublicIp.isPresent) avc.setAssignPublicIp(assignPublicIp.get)
    if (securityGroups.nonEmpty) avc.setSecurityGroups(securityGroups.asJava)
    if (subnets.nonEmpty) avc.setSubnets(subnets.asJava)

    Optional.of(avc)
  }

  protected def configureTaskOverride(c: Config): Optional[TaskOverride] = {
    if (c.isEmpty) return Optional.absent()

    val containerOverrides: Seq[ContainerOverride] =
      c.getListOrEmpty("container_overrides", classOf[Config]).asScala.map(configureContainerOverride).map(_.get)
    val executionRoleArn: Optional[String] = c.getOptional("execution_role_arn", classOf[String])
    val taskRoleArn: Optional[String] = c.getOptional("task_role_arn", classOf[String])

    val to: TaskOverride = new TaskOverride()
    if (containerOverrides.nonEmpty) to.setContainerOverrides(containerOverrides.asJava)
    if (executionRoleArn.isPresent) to.setExecutionRoleArn(executionRoleArn.get)
    if (taskRoleArn.isPresent) to.setTaskRoleArn(taskRoleArn.get)

    Optional.of(to)
  }

  protected def configureContainerOverride(c: Config): Optional[ContainerOverride] = {
    if (c.isEmpty) return Optional.absent()

    val command: Seq[String] = c.getListOrEmpty("command", classOf[String]).asScala
    val cpu: Optional[Int] = c.getOptional("cpu", classOf[Int])
    val environment: Seq[KeyValuePair] = c
      .getMapOrEmpty("environment", classOf[String], classOf[String])
      .asScala
      .map { case (k: String, v: String) => new KeyValuePair().withName(k).withValue(v) }
      .toSeq // TODO: doc
    val memory: Optional[Int] = c.getOptional("memory", classOf[Int])
    val memoryReservation: Optional[Int] = c.getOptional("memory_reservation", classOf[Int])
    val name: Optional[String] = c.getOptional("name", classOf[String])

    val co: ContainerOverride = new ContainerOverride()
    if (command.nonEmpty) co.setCommand(command.asJava)
    if (cpu.isPresent) co.setCpu(cpu.get)
    if (environment.nonEmpty) co.setEnvironment(environment.asJava)
    if (memory.isPresent) co.setMemory(memory.get)
    if (memoryReservation.isPresent) co.setMemoryReservation(memoryReservation.get)
    if (name.isPresent) co.setName(name.get)

    Optional.of(co)
  }

  protected def configurePlacementConstraint(c: Config): Optional[PlacementConstraint] = {
    if (c.isEmpty) return Optional.absent()

    val expression: Optional[String] = c.getOptional("expression", classOf[String])
    val `type`: Optional[PlacementConstraintType] = c.getOptional("type", classOf[PlacementConstraintType])

    val pc: PlacementConstraint = new PlacementConstraint()
    if (expression.isPresent) pc.setExpression(expression.get)
    if (`type`.isPresent) pc.setType(`type`.get)

    Optional.of(pc)
  }

  protected def configurePlacementStrategy(c: Config): Optional[PlacementStrategy] = {
    if (c.isEmpty) return Optional.absent()

    val field: Optional[String] = c.getOptional("field", classOf[String])
    val `type`: Optional[PlacementStrategyType] = c.getOptional("type", classOf[PlacementStrategyType])

    val ps: PlacementStrategy = new PlacementStrategy()
    if (field.isPresent) ps.setField(field.get)
    if (`type`.isPresent) ps.setType(`type`.get)

    Optional.of(ps)
  }

  override def runTask(): TaskResult = {
    val req: RunTaskRequest = buildRunTaskRequest()
    logger.debug(req.toString)
    val result: RunTaskResult = aws.withEcs(_.runTask(req))
    logger.debug(result.toString)

    val paramsToStore = cf.create()
    val last_ecs_task_run: Config = paramsToStore.getNestedOrSetEmpty("last_ecs_task_run")
    last_ecs_task_run.set("task_arns", result.getTasks.asScala.map(_.getTaskArn).asJava)

    val builder: ImmutableTaskResult.Builder = TaskResult.defaultBuilder(cf)
    builder.resetStoreParams(ImmutableList.of(ConfigKey.of("last_ecs_task_run")))
    builder.storeParams(paramsToStore)
    builder.build()
  }
}
