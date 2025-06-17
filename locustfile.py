from locust import HttpUser, task, between
import json

class EcommerceUser(HttpUser):
    # Cada usuario virtual esperará entre 1 y 5 segundos entre tareas.
    wait_time = between(1, 5)

    # NOTA: La variable 'host' se pasa desde la línea de comandos en Jenkins.
    # Esto es más flexible y se integra mejor con tu pipeline dinámico.

    def generate_static_user_data(self):
        """
        Genera datos de usuario ESTÁTICOS.
        ADVERTENCIA FUNCIONAL: Esta función devuelve SIEMPRE los mismos datos.
        La primera llamada a 'create_user' funcionará. Las siguientes probablemente
        fallarán con un error 4xx si tu base de datos tiene constraints de 'email'
        o 'username' únicos. Esto es normal para este tipo de script simple.
        """
        return {
            "firstName": "LoadTest",
            "lastName": "User",
            "email": "loadtest.user.static@test.com",
            "phone": "+1234567890",
            "imageUrl": "http://example.com/image.jpg",
            "credential": {
                "username": "loadtestuserstatic",
                "password": "password123",
                "roleBasedAuthority": "ROLE_USER",
                "isEnabled": True,
                "isAccountNonExpired": True,
                "isAccountNonLocked": True,
                "isCredentialsNonExpired": True
            }
        }

    # -- TAREAS --
    # NOTA: Todas las peticiones se hacen sin autenticación (sin token de autorización).
    # Funcionarán solo si tus endpoints son públicos.

    @task(2)  # Esta tarea se ejecutará el doble de veces que las otras.
    def create_user(self):
        """Intenta crear un nuevo usuario con datos estáticos."""
        user_data = self.generate_static_user_data()
        self.client.post(
            "/user-service/api/users", # La URL es relativa al 'host'
            json=user_data, # Locust se encarga de poner el Content-Type: application/json
            name="/user-service/api/users [POST]"
        )

    @task
    def get_users(self):
        self.client.get("/user-service/api/users", name="/user-service/api/users [GET]")

    @task
    def get_products(self):
        self.client.get("/product-service/api/products", name="/product-service/api/products")

    @task
    def get_categories(self):
        self.client.get("/product-service/api/categories", name="/product-service/api/categories")

    @task
    def get_orders(self):
        self.client.get("/order-service/api/orders", name="/order-service/api/orders")

    @task
    def get_credentials(self):
        self.client.get("/user-service/api/credentials", name="/user-service/api/credentials")

    @task
    def get_shippings(self):
        self.client.get("/shipping-service/api/shippings/", name="/shipping-service/api/shippings")

    @task
    def get_payments(self):
        self.client.get("/payment-service/api/payments/", name="/payment-service/api/payments")