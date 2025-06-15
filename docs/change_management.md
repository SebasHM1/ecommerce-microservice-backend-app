# 📌 Change Management — eCommerce Microservice Backend

## Objetivo

Definir un proceso formal para gestionar cambios en la infraestructura y los microservicios de forma segura, trazable y auditable.

---

## 🗂️ Alcance

Este proceso aplica a:
- Cambios en el código fuente de los microservicios
- Actualización de versiones Docker
- Modificación de configuración Terraform y recursos Kubernetes
- Promociones entre entornos (dev → staging → producción)

---

## ✅ Flujo de cambio aprobado

1. **Desarrollo**
   - Cada cambio debe ir en una rama `feature/` o `bugfix/`.
   - El responsable debe abrir Pull Request (PR) con descripción clara.

2. **Revisión**
   - Revisión de código obligatoria: al menos 1 aprobador diferente al autor.
   - Validación de integración local y PR pipeline.

3. **Build & Artefacto**
   - Jenkins construye y prueba los servicios.
   - Se genera una única versión de imagen Docker por commit (`repo:servicio-<commitID>`).

4. **Despliegue a DEV**
   - Jenkins aplica `terraform plan` y `terraform apply` para el namespace `dev`.

5. **Gate de Promoción a STAGING**
   - Validación manual (`input`) por equipo de Release Managers.
   - E2E tests automáticos en STAGING.
   - Validación de métricas básicas de salud.

6. **Gate de Promoción a PRODUCCIÓN**
   - Aprobación final (`input`) por CTO o responsable de cambio.
   - Despliegue automatizado vía Terraform + imágenes ya construidas (Build Once Deploy Many).

7. **Release Notes**
   - Al crear tag `vX.Y.Z` se genera un Release con changelog automático.

---

## 🔒 Auditoría

- Todo cambio queda registrado en PRs, commits y Releases.
- Todos los artefactos tienen versión única y trazable.

---

## 🔙 Rollback

- En caso de falla, se puede revertir a la imagen Docker anterior y re-aplicar Terraform con estado anterior.
- Ver [ROLLBACK_PLAN.md](ROLLBACK_PLAN.md).

---

## 👥 Roles

| Rol | Responsabilidad |
|-----|-----------------|
| **Developer** | Desarrolla, hace PR y pruebas locales |
| **Reviewer** | Revisa y aprueba PR |
| **Release Manager** | Autoriza gates a Staging y Producción |
| **CTO** | Autoriza cambios críticos y rollbacks en producción |

---

_Última actualización: 2025-06-15_
