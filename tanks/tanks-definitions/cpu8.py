from tanks import TankController, MOVE_FORWARD, MOVE_BACKWARD, TURN_LEFT, TURN_RIGHT, SHOOT,TANK_SIZE,GameState,Tank,normalize_angle
from math import degrees,atan2,sqrt
import random


class CPUTankController(TankController):
    def __init__(self, tank_id: str):
        self.tank_id = tank_id
        self.previous_enemy_count = 0

    @property
    def id(self) -> str:
        return "cpu8"

    def find_closest_enemy_tank(self, gameState: GameState) -> Tank:
        my_tank = next(tank for tank in gameState.tanks if tank.id == self.id)
        alive_enemy_tanks = [tank for tank in gameState.tanks if tank.id != self.id and tank.health > 0]

        min_distance = float('inf')
        closest_enemy = None
        for enemy_tank in alive_enemy_tanks:
            dx = enemy_tank.position[0] - my_tank.position[0]
            dy = enemy_tank.position[1] - my_tank.position[1]
            distance = sqrt(dx * dx + dy * dy)
            if distance < min_distance:
                min_distance = distance
                closest_enemy = enemy_tank

        return closest_enemy

    def find_closest_cover(self, gameState: GameState) -> tuple:
        my_tank = next(tank for tank in gameState.tanks if tank.id == self.id)
        trees = gameState.trees
        dead_tanks = [tank for tank in gameState.tanks if tank.health == 0]

        # Calculate distances from my tank to each cover object
        cover_distances = {}
        for tree in trees:
            dx = tree.position[0] - my_tank.position[0]
            dy = tree.position[1] - my_tank.position[1]
            distance = sqrt(dx * dx + dy * dy)
            cover_distances[tree] = distance

        for dead_tank in dead_tanks:
            dx = dead_tank.position[0] - my_tank.position[0]
            dy = dead_tank.position[1] - my_tank.position[1]
            distance = sqrt(dx * dx + dy * dy)
            cover_distances[dead_tank] = distance

        # Sort the distances and return the closest cover object
        closest_cover = min(cover_distances, key=cover_distances.get)
        return closest_cover.position, closest_cover.size / 2

    def decide_what_to_do_next(self, gameState: GameState) -> str:
        my_tank = next(tank for tank in gameState.tanks if tank.id == self.id)
        enemy_tank = self.find_closest_enemy_tank(gameState)
        cover_objects = gameState.trees + [tank for tank in gameState.tanks if tank.id != self.id and tank.health == 0]

        # Find the closest cover object
        min_distance = float('inf')
        closest_cover = None
        for cover in cover_objects:
            dx = cover.position[0] - my_tank.position[0]
            dy = cover.position[1] - my_tank.position[1]
            distance = sqrt(dx * dx + dy * dy)
            if distance < min_distance:
                min_distance = distance
                closest_cover = cover

        # Calculate the angle and distance to the closest cover object
        dx = closest_cover.position[0] - my_tank.position[0]
        dy = closest_cover.position[1] - my_tank.position[1]
        distance = sqrt(dx * dx + dy * dy)
        desired_angle = normalize_angle(degrees(atan2(-dy, dx)))
        angle_diff = my_tank.angle - desired_angle

        # Move towards the closest cover object
        if distance > TANK_SIZE[0]:
            if abs(angle_diff) > 6:
                return TURN_LEFT if angle_diff < 0 else TURN_RIGHT
            else:
                return MOVE_FORWARD
        else:
            # Face towards the closest alive enemy and shoot
            dx = enemy_tank.position[0] - my_tank.position[0]
            dy = enemy_tank.position[1] - my_tank.position[1]
            desired_angle = normalize_angle(degrees(atan2(-dy, dx)))
            angle_diff = my_tank.angle - desired_angle

            if abs(angle_diff) > 6:
                return TURN_LEFT if angle_diff < 0 else TURN_RIGHT
            else:
                return SHOOT


