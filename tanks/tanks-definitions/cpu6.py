from tanks import TankController, MOVE_FORWARD, MOVE_BACKWARD, TURN_LEFT, TURN_RIGHT, SHOOT,TANK_SIZE,GameState,Tank,normalize_angle
from math import degrees,atan2,sqrt
import random

class CPUTankController(TankController):
    def __init__(self, tank_id: str):
        self.tank_id = tank_id

    @property
    def id(self) -> str:
        return "cpu6"

    def find_closest_enemy_tank(self, gameState: GameState) -> Tank:
        my_tank = next(tank for tank in gameState.tanks if tank.id == self.id)
        alive_enemy_tanks = [tank for tank in gameState.tanks if tank.id != self.id and tank.health>0]
        
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

    def decide_what_to_do_next(self, gameState: GameState) -> str:
        my_tank = next(tank for tank in gameState.tanks if tank.id == self.id)
        enemy_tank = self.find_closest_enemy_tank(gameState)
        
        dx = enemy_tank.position[0] - my_tank.position[0]
        dy = enemy_tank.position[1] - my_tank.position[1]

        distance = sqrt(dx * dx + dy * dy)
        desired_angle = normalize_angle(degrees(atan2(-dy, dx)))
        angle_diff = my_tank.angle - desired_angle


        if (random.random()<0.2):
            return SHOOT
        
        if abs(angle_diff) > 5:
            return TURN_LEFT if angle_diff < 0 else TURN_RIGHT
        elif distance > max(TANK_SIZE) * 5:
            return MOVE_FORWARD
        else:
            return SHOOT
