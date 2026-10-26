import Foundation

struct CallDirectoryEntries: Codable {
    struct Identification: Codable {
        let number: Int64
        let label: String
    }

    let blocked: [Int64]
    let identified: [Identification]

    var sanitized: CallDirectoryEntries {
        let validBlocked = Array(Set(blocked.filter { $0 > 0 })).sorted()
        let validIdentified = identified
            .filter { $0.number > 0 && !$0.label.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
            .reduce(into: [Int64: Identification]()) { $0[$1.number] = $1 }
            .values
            .sorted { $0.number < $1.number }
        return CallDirectoryEntries(blocked: validBlocked, identified: validIdentified)
    }
}
