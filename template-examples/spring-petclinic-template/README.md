## install template


## Pet Clinic demo project

### Either: clone

### Or: `--load` bundled `spring-petclinic-model.json`

Since the model is loaded from a previously `--save`-ed model,
no `-f` path is needed. 


```
sift --template petclinic --load spring-petclinic-model.json`
```

The loaded system model acts just the same as a normal system model produced
by scanning classes in the repository, meaning all tree/graph options work
as normal.


Clone and build spring boot's Pet Clinic demo project

```
git clone git@github.com:spring-projects/spring-petclinic.git
mvn -f spring-petclinic compile
```
