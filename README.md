# Quarkus - Dapr
<!-- ALL-CONTRIBUTORS-BADGE:START - Do not remove or modify this section -->
[![All Contributors](https://img.shields.io/badge/all_contributors-3-orange.svg?style=flat-square)](#contributors-)
<!-- ALL-CONTRIBUTORS-BADGE:END -->

## Introduction

### What is Quarkus?

Traditional Java stacks were engineered for monolithic applications with long startup times and large memory 
requirements in a world where the cloud, containers, and Kubernetes did not exist. Java frameworks needed to evolve 
to meet the needs of this new world.

Quarkus was created to enable Java developers to create applications for a modern, cloud-native world. Quarkus is 
a Kubernetes-native Java framework tailored for GraalVM and HotSpot, crafted from best-of-breed Java libraries and 
standards. The goal is to make Java the leading platform in Kubernetes and serverless environments while offering 
developers a framework to address a wider range of distributed application architectures.

![](https://quarkus.io/assets/images/quarkus_metrics_graphic_bootmem_wide.png)

For more information about Quarkus, please go https://quarkus.io/.

### What is Dapr?

Dapr is a portable, event-driven runtime that makes it easy for any developer to build resilient, stateless and 
stateful applications that run on the cloud and edge and embraces the diversity of languages and developer frameworks. 

Leveraging the benefits of a sidecar architecture, Dapr helps you tackle the challenges that come with building 
microservices and keeps your code platform agnostic.

![](https://dapr.io/images/building-blocks.png)

For more information about Dapr, please go https://dapr.io/.

### What is Quarkus-Dapr?

Quarkus Dapr is a Quarkus extension to integrate with Dapr.

Quarkus Dapr Extension enables Java developers to create ultra lightweight Java native applications for Function 
Computing and FaaS scenes, which is also particularly suitable for running as serverless. 

With the help of Dapr, these ultra lightweight Java native applications can easily interact with external application
and resources. Dapr provides many useful building blocks to build modern distributed application: service invocation, 
state management, input/output bindings, publish & subscribe, secret management......

Because of the advantages of sidecar model, the native applications can benefit from Dapr's distributed capabilities
while remain lightweight without introducing too many dependencies. This is not only helping to keep the size of java
native applications, but also making the native applications easy to build as native images.

## Examples

With Quarkus Dapr Extension, it's pretty easy for java developers.

### publish & subscribe

To publish events to your message broker, just inject a dapr client to your bean and call it's publishEvent() method:

```java
    @Inject
    SyncDaprClient dapr;

    dapr.publishEvent("messagebus", "topic1", content.getBytes(StandardCharsets.UTF_8), new HashMap<>());
```

To subscribe events for your message broker, adding some annotations on your method is enough:

```java
@POST
@Path("/topic1")
@Topic(name = "topic1", pubsubName = "messagebus")
public String eventOnTopic2(String content) {......}
```

For more details and hands-on experiences, please reference to our [Demo](./demo/README.md).

## Contributors ✨

Thanks goes to these wonderful people ([emoji key](https://allcontributors.org/docs/en/emoji-key)):

<!-- ALL-CONTRIBUTORS-LIST:START - Do not remove or modify this section -->
<!-- prettier-ignore-start -->
<!-- markdownlint-disable -->
<table>
  <tr>
    <td align="center"><a href="https://zhfeng.github.io/"><img src="https://avatars.githubusercontent.com/u/1246139?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Amos Feng</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-dapr/commits?author=zhfeng" title="Code">💻</a> <a href="#maintenance-zhfeng" title="Maintenance">🚧</a></td>
    <td align="center"><a href="https://www.naah69.com"><img src="https://avatars.githubusercontent.com/u/25682169?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Naah</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-dapr/commits?author=naah69" title="Code">💻</a> <a href="#maintenance-naah69" title="Maintenance">🚧</a></td>
    <td align="center"><a href="https://skyao.io"><img src="https://avatars.githubusercontent.com/u/1582369?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Sky Ao</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-dapr/commits?author=skyao" title="Code">💻</a> <a href="#maintenance-skyao" title="Maintenance">🚧</a></td>
  </tr>
</table>

<!-- markdownlint-restore -->
<!-- prettier-ignore-end -->

<!-- ALL-CONTRIBUTORS-LIST:END -->

This project follows the [all-contributors](https://github.com/all-contributors/all-contributors) specification. Contributions of any kind welcome!