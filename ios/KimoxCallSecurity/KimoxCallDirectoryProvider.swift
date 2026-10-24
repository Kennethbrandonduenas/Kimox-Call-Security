import CallKit

final class KimoxCallDirectoryProvider: CXCallDirectoryProvider {
    override func beginRequest(with context: CXCallDirectoryExtensionContext) {
        // Fase iOS: cargar aquí la lista local normalizada desde App Group.
        // iOS requiere que los números estén precargados; no consulta un backend por llamada.
        context.completeRequest()
    }
}
