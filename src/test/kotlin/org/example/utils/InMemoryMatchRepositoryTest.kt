package org.example.utils

class InMemoryMatchRepositoryTest : MatchRepositoryTest() {
    override fun createRepo() = InMemoryMatchRepository()
}
