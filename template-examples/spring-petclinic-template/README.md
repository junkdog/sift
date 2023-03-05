A basic template for the [Spring Boot Pet Clinic][petclinic] demo application.

#### This template:
- uses `includes(template)` for registering controller and repository entities.
- defines a new entity type, `E.modelAttributes`, for `@ModelAttribute` methods.
- wires all entity relationships together, primarily by identifying invocations.
- graphviz property configuration for `--render`.

![petclinic](../../docs/images/sift-example-template-petclinic.png)
 

## install template

Install the template by running by running main() in Install.kt


## Pet Clinic demo project

### Either: clone

Clone and build spring boot's Pet Clinic demo project.

```
git clone git@github.com:spring-projects/spring-petclinic.git
mvn -f spring-petclinic package
```

### Or: `--load` bundled `spring-petclinic-model.json`

Since the model is loaded from a previously `--save`-ed model,
no `-f` path is needed. 

```
sift --template petclinic --load spring-petclinic-model.json`
```

The loaded system model acts just the same as a normal system model produced
by scanning classes in the repository, meaning all tree/graph options work
as normal.


 [petclinic]: https://github.com/spring-projects/spring-petclinic