package ai.linfra.resource_app.firebase.impl

import ai.linfra.resource_app.user.domain.dependencies.CreateFirebaseUser
import ai.linfra.resource_app.user.domain.model.UserCreateCommand
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserRecord
import com.google.firebase.auth.UserRecord.CreateRequest
import org.springframework.stereotype.Component

@Component
class CreateFirebaseUserImpl(
): CreateFirebaseUser {

    override fun createFirebaseUser(user: UserCreateCommand): String {
        val userRecord: UserRecord = FirebaseAuth.getInstance().createUser(createUSerRecord(user))

        return userRecord.uid
    }

    private fun createUSerRecord(user: UserCreateCommand): CreateRequest {
        return CreateRequest()
            .setEmail(user.email)
            .setEmailVerified(true)
            .setPassword(user.password)
            .setDisplayName(user.fullName)
            .setDisabled(false);
    }
}