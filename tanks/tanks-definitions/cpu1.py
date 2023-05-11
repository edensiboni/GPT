from tanks import TankController, MOVE_FORWARD, MOVE_BACKWARD, TURN_LEFT, TURN_RIGHT, SHOOT,TANK_SIZE,GameState,Tank,normalize_angle
from math import degrees,atan2,sqrt
import random

class CPUTankController(TankController):
    def __init__(self, tank_id: str):
        self.tank_id = tank_id

    @property
    def id(self) -> str:
        return "cpu1"


    def decide_what_to_do_next(self, gameState: GameState) -> str:
        if (random.random()<0.2):
            return SHOOT
        if (random.random()<0.2):
            return TURN_RIGHT
        return MOVE_FORWARD

