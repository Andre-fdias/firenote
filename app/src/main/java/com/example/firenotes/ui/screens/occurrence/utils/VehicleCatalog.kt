package com.example.firenotes.ui.screens.occurrence.utils

object VehicleCatalog {
    val brands = listOf(
        "Chevrolet",
        "Fiat",
        "Volkswagen",
        "Ford",
        "Toyota",
        "Hyundai",
        "Honda",
        "Renault",
        "Jeep",
        "Nissan",
        "Mitsubishi",
        "Peugeot",
        "Citroën",
        "Caoa Chery",
        "Kia",
        "BMW",
        "Mercedes-Benz",
        "Audi",
        "Volvo",
        "Land Rover",
        "Suzuki",
        "Subaru",
        "Outros"
    ).sorted()

    val modelsByBrand = mapOf(
        "Chevrolet" to listOf("Onix", "Prisma", "Cruze", "Tracker", "S10", "Spin", "Equinox", "Trailblazer", "Celta", "Corsa", "Astra", "Vectra", "Monza", "Meriva", "Zafira", "Classic", "Cobalt", "Montana"),
        "Fiat" to listOf("Uno", "Palio", "Argo", "Cronos", "Mobi", "Strada", "Toro", "Fiorino", "Pulse", "Fastback", "Siena", "Grand Siena", "Punto", "Idea", "Stilo", "Bravo", "Linea", "Doblo", "Ducato"),
        "Volkswagen" to listOf("Gol", "Polo", "Virtus", "T-Cross", "Nivus", "Taos", "Tiguan", "Amarok", "Jetta", "Passat", "Golf", "Voyage", "Saveiro", "Fox", "Up!", "Fusca", "Kombi", "Santana", "Bora"),
        "Ford" to listOf("Ka", "Fiesta", "Focus", "Fusion", "EcoSport", "Ranger", "Territory", "Bronco", "Maverick", "Mustang", "Escort", "Mondeo", "Edge", "Courier"),
        "Toyota" to listOf("Corolla", "Hilux", "SW4", "Yaris", "Etios", "Corolla Cross", "RAV4", "Prius", "Camry", "Land Cruiser"),
        "Hyundai" to listOf("HB20", "HB20S", "Creta", "Tucson", "ix35", "Santa Fe", "Elantra", "i30", "Azera", "HR", "Veloster"),
        "Honda" to listOf("Civic", "Fit", "City", "HR-V", "WR-V", "CR-V", "Accord", "Biz", "CG 160", "CB 300"),
        "Renault" to listOf("Sandero", "Logan", "Duster", "Kwid", "Oroch", "Captur", "Fluence", "Megane", "Clio", "Scenic", "Master", "Kardian"),
        "Jeep" to listOf("Renegade", "Compass", "Commander", "Grand Cherokee", "Wrangler"),
        "Nissan" to listOf("Kicks", "Versa", "Sentra", "Frontier", "March", "Tiida", "Livina"),
        "Mitsubishi" to listOf("L200 Triton", "ASX", "Outlander", "Pajero", "Eclipse Cross", "Lancer"),
        "Peugeot" to listOf("208", "2008", "3008", "5008", "Partner", "Expert", "Boxer", "206", "207", "308"),
        "Citroën" to listOf("C3", "C4 Cactus", "C4 Lounge", "C3 Aircross", "Jumpy", "Jumper", "C5"),
        "Caoa Chery" to listOf("Tiggo 2", "Tiggo 3X", "Tiggo 5X", "Tiggo 7", "Tiggo 8", "Arrizo 5", "Arrizo 6"),
        "Kia" to listOf("Sportage", "Cerato", "Picanto", "Sorento", "Soul", "Bongo", "Niro", "Carnival"),
        "BMW" to listOf("Série 3", "Série 5", "Série 1", "X1", "X3", "X5", "X6", "M3"),
        "Mercedes-Benz" to listOf("Classe A", "Classe C", "Classe E", "GLA", "GLC", "GLE", "Sprinter"),
        "Audi" to listOf("A3", "A4", "A5", "Q3", "Q5", "Q7", "e-tron"),
        "Volvo" to listOf("XC40", "XC60", "XC90", "V40", "S60"),
        "Land Rover" to listOf("Evoque", "Discovery", "Defender", "Range Rover Sport", "Velar"),
        "Suzuki" to listOf("Jimny", "Vitara", "Grand Vitara", "Swift"),
        "Subaru" to listOf("Impreza", "XV", "Forester", "Outback")
    )
}
