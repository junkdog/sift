package example.petclinic

import sift.core.template.save

// project pom is configured to run this during maven's `install` phase:
// saves "petclinic" template to ~/.local/share/sift/templates/petclinic.json
fun main() {
    PetClinicTemplate().save()
}