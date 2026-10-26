import CallKit
import Foundation

enum KimoxCallDirectoryStore {
    static let appGroup = "group.com.kimoxcallsecurity.shared"
    static let extensionIdentifier = "com.kimoxcallsecurity.KimoxCallDirectory"
    private static let entriesKey = "kimox.callDirectory.entries.v1"

    static func save(blocked: [String], identified: [String: String]) throws {
        let blockedNumbers = blocked.compactMap(normalize)
        let labels = identified.compactMap { rawNumber, label -> CallDirectoryEntries.Identification? in
            guard let number = normalize(rawNumber), !label.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return nil }
            return CallDirectoryEntries.Identification(number: number, label: String(label.prefix(256)))
        }
        let entries = CallDirectoryEntries(blocked: blockedNumbers, identified: labels).sanitized
        guard let defaults = UserDefaults(suiteName: appGroup) else {
            throw NSError(domain: "KimoxCallDirectory", code: 1, userInfo: [NSLocalizedDescriptionKey: "App Group no configurado"])
        }
        defaults.set(try JSONEncoder().encode(entries), forKey: entriesKey)
        defaults.set(Date().timeIntervalSince1970, forKey: "kimox.callDirectory.lastSyncAt")
        CXCallDirectoryManager.sharedInstance.reloadExtension(withIdentifier: extensionIdentifier)
    }

    private static func normalize(_ raw: String) -> Int64? {
        let digits = raw.filter(\.isNumber)
        guard digits.count >= 7, let number = Int64(digits), number > 0 else { return nil }
        return number
    }
}
