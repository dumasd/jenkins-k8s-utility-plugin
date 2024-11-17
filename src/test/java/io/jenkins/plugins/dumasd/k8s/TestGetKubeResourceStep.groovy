pipeline {
    agent any

    stages {
        stage('Hello') {
            steps {
                script {
                    def resourcesMap = [:]

                    def result1 = kubeResourceGet(credentialsId: 'b59dab5d-9c6f-4962-b3a2-e5c121b0aee4', items: [
                            [apiVersion: 'apps/v1', kind: 'Deployment', name: 'ms-site', namespace: 'membersite'],
                            [apiVersion: 'networking.istio.io/v1alpha3', kind: 'DestinationRule', name: 'ms-site-destination-rule', namespace: 'membersite']
                    ])
                    def result2 = kubeResourceGet(credentialsId: 'b59dab5d-9c6f-4962-b3a2-e5c121b0aee4', items: [
                            [apiVersion: 'apps/v1', kind: 'Deployment', name: 'ms-bill', namespace: 'membersite'],
                            [apiVersion: 'networking.istio.io/v1alpha3', kind: 'DestinationRule', name: 'ms-bill-destination-rule', namespace: 'membersite']
                    ])

                    resourcesMap['ms-site'] = result1
                    resourcesMap['ms-bill'] = result2

                    def alteredResourcesMap = input(message: "Kubernetes Resource Edit", parameters: [
                            kubeResourceAlter(name: 'ALTER', itemsMap: resourcesMap)
                    ])

                    alteredResourcesMap.each { entry ->
                        echo "Appling workload resource : ${entry.key}"
                        kubeResourceApply(credentialsId: 'b59dab5d-9c6f-4962-b3a2-e5c121b0aee4', items: entry.value)
                    }

                }
            }
        }
    }

}


