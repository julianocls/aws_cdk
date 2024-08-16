package com.myorg;


import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.applicationautoscaling.EnableScalingProps;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.logs.LogGroup;
import software.constructs.Construct;

public class Service01Stack extends Stack {

    private static final int PORT = 8080;
    private static final String SERVICE_NAME = "Service01";
    private static final String LOG_GROUP = "Service01LogGroup";
    private static final String AWS_PROJECT_NAME = "julianoclsantos/curso_aws_project01:1.0.0";
    private static final String STATUS_CODE = "200";
    private static final String CONTAINER_NAME = "aws_project01";
    private static final String ACTUATOR_HEALTH = "/actuator/health";
    private static final int MIN_CAPACITY = 2;
    private static final int MAX_CAPACITY = 4;
    private static final int TARGET_UTILIZATION_PERCENT = 50;
    private static final int SECONDS_DURATION = 60;

    public Service01Stack(final Construct scope, final String id, Cluster cluster) {
        this(scope, id, null, cluster);
    }

    public Service01Stack(final Construct scope, final String id, final StackProps props, Cluster cluster) {
        super(scope, id, props);

        ApplicationLoadBalancedFargateService service01 = ApplicationLoadBalancedFargateService.Builder.create(this, "ALB01")
                .serviceName("service-01")
                .cluster(cluster)
                .cpu(512)
                .memoryLimitMiB(1024)
                .desiredCount(MIN_CAPACITY)
                .listenerPort(PORT)
                .assignPublicIp(true)
                .taskImageOptions(
                        ApplicationLoadBalancedTaskImageOptions.builder()
                                .containerName(CONTAINER_NAME)
                                .image(ContainerImage.fromRegistry(AWS_PROJECT_NAME))
                                .containerPort(PORT)
                                .logDriver(LogDriver.awsLogs(AwsLogDriverProps.builder()
                                                .logGroup(LogGroup.Builder.create(this, LOG_GROUP)
                                                        .logGroupName(SERVICE_NAME)
                                                        .removalPolicy(RemovalPolicy.DESTROY)
                                                        .build())
                                                .streamPrefix(SERVICE_NAME)
                                                .build()
                                        )
                                )
                                .build()
                )
                .publicLoadBalancer(true)
                .build();

        service01.getTargetGroup().configureHealthCheck(new HealthCheck.Builder()
                .path(ACTUATOR_HEALTH)
                        .port(String.valueOf(PORT))
                        .healthyHttpCodes(STATUS_CODE)
                .build());

        ScalableTaskCount scalableTaskCount = service01.getService().autoScaleTaskCount(EnableScalingProps.builder()
                        .minCapacity(MIN_CAPACITY)
                        .maxCapacity(MAX_CAPACITY)
                .build());

        scalableTaskCount.scaleOnCpuUtilization("Service01AutoScaling", CpuUtilizationScalingProps.builder()
                        .targetUtilizationPercent(TARGET_UTILIZATION_PERCENT)
                        .scaleInCooldown(Duration.seconds(SECONDS_DURATION))
                        .scaleOutCooldown(Duration.seconds(SECONDS_DURATION))
                .build());

    }

}
