package com.myorg;

import software.amazon.awscdk.App;

public class AwsCdkApp {
    public static void main(final String[] args) {
        App app = new App();

        var vpcStack = new VpcStack(app, "Vpc");

        var clusterStack = new ClusterStack(app, "Cluster", vpcStack.getVpc());
        clusterStack.addDependency(vpcStack);

        app.synth();
    }
}

