# go4-backend

This project uses [Gradle](https://gradle.org/).
To build and run the application, use the *Gradle* tool window by clicking the Gradle icon in the right-hand toolbar,
or run it directly from the terminal:

* Run `./gradlew run` to build and run the application.
* Run `./gradlew build` to only build the application.
* Run `./gradlew check` to run all checks, including tests.
* Run `./gradlew clean` to clean all build outputs.

Note the usage of the Gradle Wrapper (`./gradlew`).
This is the suggested way to use Gradle in production projects.

[Learn more about the Gradle Wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html).

[Learn more about Gradle tasks](https://docs.gradle.org/current/userguide/command_line_interface.html#common_tasks).

This project follows the suggested multi-module setup and consists of the `app` and `utils` subprojects.
The shared build logic was extracted to a convention plugin located in `buildSrc`.

This project uses a version catalog (see `gradle/libs.versions.toml`) to declare and version dependencies
and both a build cache and a configuration cache (see `gradle.properties`).


Relações Principais:

✅ User 1:1 Company (owner/ownedCompany)
✅ User 1:N CompanyMember (user pode ser membro de várias empresas)
✅ Company 1:N CompanyMember (empresa tem vários membros)
✅ Unique constraint em CompanyMember (company_id, user_id)
✅ Invitation referencia Company e User via FK



## 🎯 Fluxo de Autenticação

### Login SMS (Principal)
1. `POST /v1/auth/login` → Envia SMS
2. `POST /v1/auth/verify-sms` → Verifica código → Token JWT

### Login Email (Fallback)
1. `POST /v1/auth/login-email` → Envia email
2. `POST /v1/auth/verify-email` → Verifica código → Token JWT

### Registro
1. `POST /v1/auth/register` → Cria usuário + SMS
2. `POST /v1/auth/activate` → Ativa conta → Token JWT

### Social Login (OAuth2)
1. Redirect para provider (Google/Apple/Microsoft)
2. Callback para OAuth2SuccessHandler
3. Processar usuário OAuth2
4. Retornar JWT via deep link