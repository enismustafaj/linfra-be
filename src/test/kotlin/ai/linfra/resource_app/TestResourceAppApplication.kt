package ai.linfra.resource_app

import org.springframework.boot.fromApplication
import org.springframework.boot.with


fun main(args: Array<String>) {
	fromApplication<ResourceAppApplication>().with(TestcontainersConfiguration::class).run(*args)
}
