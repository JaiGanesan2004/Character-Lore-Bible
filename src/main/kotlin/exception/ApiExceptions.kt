package exception

class UnauthorizedException(message: String = "Unauthorized") : RuntimeException(message)

class NotFoundException(message: String = "Resource not found") : RuntimeException(message)

class BadRequestException(message: String = "Bad request") : RuntimeException(message)

class ForbiddenException(message: String = "Forbidden") : RuntimeException(message)