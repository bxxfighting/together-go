# -*- encoding=utf8 -*-
__author__ = "xx"

from airtest.core.api import *

auto_setup(__file__)

dev = device()

TYP_CATCH = 1
TYP_DRUM = 2

def is_start_catch():
    try:
        if wait(Template(r"tpl1569426258398.png", record_pos=(-0.001, 0.632), resolution=(1080, 1920)), timeout=1):
            return True
    except:
        return False


def throw_ball():
    site_list = [(550, 1700), (250, 1600), (555, 1500), (850, 1600), (550, 1700), (250, 1500)]
    dev.minitouch.swipe_along(site_list, duration=0.5)
    # swipe((555, 1700), (555, 900), duration=0.1)
    
def is_hold():
    try:
        if wait(Template(r"tpl1569424531942.png", record_pos=(0.003, -0.006), resolution=(1080, 1920)), timeout=10):
                touch(Template(r"tpl1569424624353.png", record_pos=(0.008, 0.276), resolution=(1080, 1920)))
                return True
    except:
        return False
    
def hold_back():
    back()
    back()
    back()
    try:
        if wait(Template(r"tpl1569424711527.png", record_pos=(-0.018, 0.76), resolution=(1080, 1920)), timeout=10):
            touch(Template(r"tpl1569424781833.png", record_pos=(-0.207, 0.781), resolution=(1080, 1920)))
    except:
        pass
    
    
def back():
    keyevent("BACK")

def catch_pet():
    while True:
        if not is_start_catch():
            continue
        throw_ball()
        if is_hold():
            hold_back()
            break
            
def is_drum():
    try:
        if wait(Template(r"tpl1569427585001.png", record_pos=(0.017, 0.35), resolution=(1080, 1920)), timeout=1):
            return True
    except:
        return False

def play_drum():
    if is_drum():
        touch(Template(r"tpl1569427630812.png", record_pos=(0.009, 0.244), resolution=(1080, 1920)))
        back()
    
def is_main():
    sleep(3)
    try:
        if wait(Template(r"tpl1569427708904.png", record_pos=(-0.001, 0.761), resolution=(1080, 1920)), timeout=1):
            return True
    except:
        return False


def is_what():
    count = 0
    while True:
        try:
            if is_drum():
                return TYP_DRUM
            elif is_start_catch():
                return TYP_CATCH
        except:
            pass
        count += 1
        if count > 10:
            return None

def click_around():
    min_x = 380
    max_x = 680
    min_y = 1000
    max_y = 1300
    for i in range(5):
        for j in range(5):
            touch((min_x + i * 60 + 30, min_y + j * 60 + 30))
            if not is_main():
                break
    return True

def run():
    while True:
        if click_around():
            continue
        typ = is_what()
        if typ == TYP_DRUM:
            play_drum()
        elif typ == TYP_CATCH:
            catch_pet()
        else:
            back()
    
run()
